package org.odata4j.edm;

import java.util.ArrayList;
import java.util.List;

import org.core4j.Enumerable;
import org.core4j.Predicate1;
import org.odata4j.core.ImmutableList;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OPredicates;
import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.EdmFunctionImport.FunctionKind;
import org.odata4j.edm.EdmItem.BuilderContext;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.internal.AndroidCompat;

/**
 * The &lt;edmx:DataServices&gt; element contains the service metadata of a Data Service. This service metadata contains zero or more EDM conceptual schemas.
 *
 * <p>Since this is the root of a large metadata tree, convenience methods are included to help locate child metadata elements.</p>
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd541087(v=prot.10).aspx">[msdn] 2.2 &lt;edmx:DataServices&gt;</a>
 */
public class EdmDataServices {

  public static final EdmDataServices EMPTY = new EdmDataServices(null, ImmutableList.<EdmSchema> create(), ImmutableList.<PrefixedNamespace> create());

  private final ODataVersion version;
  private final ImmutableList<EdmSchema> schemas;
  private final ImmutableList<PrefixedNamespace> namespaces;

  protected EdmDataServices(ODataVersion version, ImmutableList<EdmSchema> schemas, ImmutableList<PrefixedNamespace> namespaces) {
    this.version = version;
    this.schemas = schemas;
    this.namespaces = namespaces;
  }

  public String getVersion() {
    return version != null ? version.asString : null;
  }

  public ImmutableList<EdmSchema> getSchemas() {
    return schemas;
  }

  public ImmutableList<PrefixedNamespace> getNamespaces() {
    return namespaces;
  }

  public EdmEntitySet getEdmEntitySet(String entitySetName) {
    EdmEntitySet ees = findEdmEntitySet(entitySetName);
    if (ees != null) {
      return ees;
    }
    throw new NotFoundException("EdmEntitySet " + entitySetName + " is not found");
  }

  public EdmEntitySet getEdmEntitySet(final EdmEntityType type) {
    if (type == null)
      throw new IllegalArgumentException("type cannot be null");
    EdmEntitySet ees = Enumerable.create(getEntitySets())
        .firstOrNull(new Predicate1<EdmEntitySet>() {
          @Override
          public boolean apply(EdmEntitySet input) {
            return type.equals(input.getType());
          }
        });

    if (ees != null) {
      return ees;
    }
    throw new NotFoundException("EdmEntitySet " + type.getName() + " is not found");
  }

  public EdmEntitySet findEdmEntitySet(String entitySetName) {
    int idx = entitySetName.indexOf('.');
    if (idx != -1) {
      EdmEntitySet ees = findEdmEntitySet(entitySetName.substring(0, idx), entitySetName.substring(idx+1));
      if (ees != null) {
        return ees;
      }
    }
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmEntitySet ees : eec.getEntitySets()) {
          if (ees.getName().equals(entitySetName)) {
            return ees;
          }
        }
      }
    }
    return null;
  }
  
  private EdmEntitySet findEdmEntitySet(String entityContainerName, String entitySetName) {
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        if (!eec.getName().equals(entityContainerName)) {
          continue;
        }
        for (EdmEntitySet ees : eec.getEntitySets()) {
          if (ees.getName().equals(entitySetName)) {
            return ees;
          }
        }
      }
    }
    return null;
  }

  public EdmAssociationSet findEdmAssociationSet(String associationSetName) {
    
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmAssociationSet eas : eec.getAssociationSets()) {
          if (eas.getName().equals(associationSetName)) {
            return eas;
          }
        }
      }
    }
    return null;
  }
  
  public boolean containsEdmFunctionImport(String functionImportName){
    int dotPos = functionImportName.indexOf(".");
    String schemaName = null;
    if (dotPos > 0){
      // We have a fully-qualified name
      schemaName = functionImportName.substring(0, dotPos);
      functionImportName = functionImportName.substring(dotPos + 1); 
    }
    for (EdmSchema schema : this.schemas) {
      if (schemaName == null || schemaName.equals(schema.getNamespace())){
        for (EdmEntityContainer eec : schema.getEntityContainers()) {
          for (EdmFunctionImport efi : eec.getFunctionImports()) {
            if (efi.getName().equals(functionImportName)) {
              return true;
            }
          }
        }
      }  
    }
    return false;  
  }
  
  public EdmFunctionImport findEdmFunctionImport(String functionImportName) {
    return findEdmFunctionImport(functionImportName, null);
  }
  
  public EdmFunctionImport findEdmFunctionImport(String functionImportName, EdmType bindingType) {
    return findEdmFunctionImport(functionImportName, bindingType, null);
  }
  
  public EdmFunctionImport findEdmFunctionImport(String functionImportName, EdmType bindingType, EdmFunctionImport.FunctionKind functionKind) {
    int dotPos = functionImportName.indexOf(".");
    String schemaName = null;
    if (dotPos > 0){
      // We have a fully-qualified name
      schemaName = functionImportName.substring(0, dotPos);
      functionImportName = functionImportName.substring(dotPos + 1); 
    }
    List<EdmFunctionImport> matchingFunctions = new ArrayList<EdmFunctionImport>();
    for (EdmSchema schema : this.schemas) {
      if (schemaName == null || schemaName.equals(schema.getNamespace())){
        for (EdmEntityContainer eec : schema.getEntityContainers()) {
          for (EdmFunctionImport efi : eec.getFunctionImports()) {
            if (efi.getName().equals(functionImportName)){
              if ((bindingType != null && efi.isBindable() && efi.getBoundParameter().getType().equals(bindingType)) 
                  || bindingType == null){
                if (functionKind == null || (functionKind.equals(efi.getFunctionKind()))){
                  matchingFunctions.add(efi);
                }
              }
            }
          }
        }
      }
    }
    if (matchingFunctions.size() == 1){
      return matchingFunctions.get(0);
    } else if (matchingFunctions.size() > 1){
      throw new BadRequestException("Ambiguous call to function : '" + functionImportName + "', multiple functions match parameters.");
    } else {
      return null;
    }
  }

    
  public String getSchemaNamespaceOfEdmEntitySet(EdmEntitySet entitySet) {
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmEntitySet ees : eec.getEntitySets()) {
          if (ees.equals(entitySet)) {
            return schema.getNamespace();
          }
        }
      }
    }
    return null;
  }
  
  public List<EdmFunctionImport> findBindableEdmFunctionImport(EdmType boundingType){
    List<EdmFunctionImport> result = new ArrayList<EdmFunctionImport>();
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmFunctionImport efi : eec.getFunctionImports()) {       
          if (efi.isBindable()) {
            EdmFunctionParameter param = efi.getBoundParameter();
            if (param != null && param.getType().equals(boundingType)) {
            result.add(efi);
            }
          }
        }
      }
    }
    return result;
  }

  public EdmComplexType findEdmComplexType(String complexTypeFQName) {
    for (EdmSchema schema : this.schemas) {
      for (EdmComplexType ect : schema.getComplexTypes()) {
        if (ect.getFullyQualifiedTypeName().equals(complexTypeFQName)) {
          return ect;
        }
      }
    }
    return null;
  }

  public EdmType findEdmEntityType(String fqName) {
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityType et : schema.getEntityTypes()) {
        if (et.getFullyQualifiedTypeName().equals(fqName)) {
          return et;
        }
      }
    }
    return null;
  }

  public EdmPropertyBase findEdmProperty(String propName) {
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmEntitySet ees : eec.getEntitySets()) {
          for (EdmNavigationProperty ep : ees.getType().getNavigationProperties()) {
            if (ep.getName().equals(propName)) {
              return ep;
            }
          }
          for (final EdmProperty ep : ees.getType().getProperties()) {
            if (ep.getName().equals(propName)) {
              return ep;
            }
          }
        }
      }
    }
    return null;
  }

  // - - - - - - - - - - -  - -
  public EdmAssociation findEdmAssociation(String fqName) {
    for (EdmSchema schema : this.schemas) {
      for (EdmAssociation assoc : schema.getAssociations()) {
        if (assoc.getFQNamespaceName().equals(fqName)) {
          return assoc;
        }
      }
    }
    return null;
  }

  // - - - - - - - - - - - - - - - -

  public Iterable<EdmEntityType> getEntityTypes() {
    List<EdmEntityType> rt = new ArrayList<EdmEntityType>();
    for (EdmSchema schema : this.schemas) {
      rt.addAll(schema.getEntityTypes());
    }
    return rt;
  }

  public Iterable<EdmComplexType> getComplexTypes() {
    List<EdmComplexType> rt = new ArrayList<EdmComplexType>();
    for (EdmSchema schema : this.schemas) {
      rt.addAll(schema.getComplexTypes());
    }
    return rt;
  }

  public Iterable<EdmStructuralType> getStructuralTypes() {
    return Enumerable.create(getEntityTypes()).cast(EdmStructuralType.class)
        .concat(Enumerable.create(getComplexTypes()).cast(EdmStructuralType.class));
  }

  public Iterable<EdmAssociation> getAssociations() {
    List<EdmAssociation> rt = new ArrayList<EdmAssociation>();
    for (EdmSchema schema : this.schemas) {
      rt.addAll(schema.getAssociations());
    }
    return rt;
  }

  public Iterable<EdmEntitySet> getEntitySets() {
    List<EdmEntitySet> rt = new ArrayList<EdmEntitySet>();
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        rt.addAll(eec.getEntitySets());
      }
    }
    return rt;
  }

  public Iterable<EdmFunctionImport> getFunctions(FunctionKind functionKind) {
    List<EdmFunctionImport> rt = new ArrayList<EdmFunctionImport>();
    for (EdmSchema schema : this.schemas) {
      for (EdmEntityContainer eec : schema.getEntityContainers()) {
        for (EdmFunctionImport efi : eec.getFunctionImports()) {
          if (efi.getFunctionKind() == functionKind) {
            rt.add(efi);
          }
        }
      }
    }
    return rt;
  }
  
  public EdmSchema findSchema(String namespace) {
    for (EdmSchema schema : this.schemas) {
      if (schema.getNamespace().equals(namespace)) {
        return schema;
      }
    }
    return null;
  }

  public Iterable<EdmStructuralType> getSubTypes(EdmStructuralType t) {
    return Enumerable.create(getStructuralTypes()).where(OPredicates.edmSubTypeOf(t));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(EdmDataServices metadata) {
    Builder builder = new Builder();
    BuilderContext context = new BuilderContext(builder);
    List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
    for (EdmSchema schema : metadata.schemas)
      schemas.add(EdmSchema.newBuilder(schema, context));
    return builder.setVersion(metadata.version).addSchemas(schemas).addNamespaces(metadata.namespaces);
  }

  public EdmType resolveType(String fqTypeName) {
    boolean isCollection = false;
    String collectionPrefix = "Collection(";
    if (fqTypeName.startsWith(collectionPrefix)) {
      isCollection = true;
      fqTypeName = fqTypeName.substring(collectionPrefix.length(), fqTypeName.length()-1);
    }
    EdmType t = EdmType.getSimple(fqTypeName);
    if (t == null) {
      // not simple, try complex
      t = this.findEdmComplexType(fqTypeName);
      if (t == null) {
        // try entity type
        t = this.findEdmEntityType(fqTypeName);
      }
    }
    
    if(isCollection && t != null) {
      t = new EdmCollectionType(CollectionKind.Collection, t);
    }
    return t;
  }

  /** Mutable builder for {@link EdmDataServices} objects. */
  public static class Builder {

    private ODataVersion version = ODataConstants.DATA_SERVICE_VERSION;
    private final List<EdmSchema.Builder> schemas = new ArrayList<EdmSchema.Builder>();
    private final List<PrefixedNamespace> namespaces = new ArrayList<PrefixedNamespace>();

    public EdmDataServices build() {
      List<EdmSchema> schemas = new ArrayList<EdmSchema>(this.schemas.size());
      for (EdmSchema.Builder schema : this.schemas)
        schemas.add(schema.build());
      return new EdmDataServices(version, ImmutableList.copyOf(schemas), ImmutableList.copyOf(namespaces));
    }

    public Builder setVersion(ODataVersion version) {
      this.version = version;
      return this;
    }

    public Builder addSchemas(List<EdmSchema.Builder> schemas) {
      this.schemas.addAll(schemas);
      return this;
    }

    public Builder addNamespaces(List<PrefixedNamespace> namespaces) {
      if (namespaces != null)
        this.namespaces.addAll(namespaces);
      return this;
    }

    public Builder addSchemas(EdmSchema.Builder... schemas) {
      for (EdmSchema.Builder schema : schemas)
        this.schemas.add(schema);
      return this;
    }

    public List<EdmSchema.Builder> getSchemas() {
      return schemas;
    }

    public EdmComplexType.Builder findEdmComplexType(String complexTypeFQName) {
      // TODO share or remove
      for (EdmSchema.Builder schema : this.schemas) {
        String fqName = schema.dealias(complexTypeFQName);
        for (EdmComplexType.Builder ect : schema.getComplexTypes()) {
          if (ect.getFullyQualifiedTypeName().equals(fqName)) {
            return ect;
          }
        }
      }
      return null;
    }

    public Iterable<EdmEntityType.Builder> getEntityTypes() {
      // TODO share or remove
      List<EdmEntityType.Builder> rt = new ArrayList<EdmEntityType.Builder>();
      for (EdmSchema.Builder schema : this.schemas) {
        rt.addAll(schema.getEntityTypes());
      }
      return rt;
    }

    public Iterable<EdmAssociation.Builder> getAssociations() {
      // TODO share or remove
      List<EdmAssociation.Builder> rt = new ArrayList<EdmAssociation.Builder>();
      for (EdmSchema.Builder schema : this.schemas) {
        rt.addAll(schema.getAssociations());
      }
      return rt;
    }

    public EdmEntityType.Builder findEdmEntityType(String fqName) {
      // TODO share or remove
      if (fqName == null)
        return null;
      for (EdmSchema.Builder schema : this.schemas) {
        for (EdmEntityType.Builder et : schema.getEntityTypes()) {
          if (fqName.equals(et.getFQAliasName()) || et.getFullyQualifiedTypeName().equals(fqName)) {
            return et;
          }
        }
      }
      return null;
    }

    public EdmSchema.Builder findSchema(String namespace) {
      // TODO share or remove
      for (EdmSchema.Builder schema : this.schemas) {
        if (schema.getNamespace().equals(namespace)) {
          return schema;
        }
      }
      return null;
    }

    public EdmType.Builder<?, ?> resolveType(String fqTypeName) {
      if (fqTypeName == null || AndroidCompat.String_isEmpty(fqTypeName))
        return null;
      // type resolution:
      // NOTE: this will likely change if RowType is ever implemented. I'm
      //       guessing that in that case, the TempEdmFunctionImport will already
      //       have a EdmRowType instance it built during parsing.
      // first, try to resolve the type name as a simple or complex type
      
      // Is it a collection ?
      if (fqTypeName.endsWith("")){
        int parenthesisPos = fqTypeName.indexOf("(");
        if (parenthesisPos > 0){
          String collectionKindS = fqTypeName.substring(0, parenthesisPos);
          CollectionKind collectionKind = null;
          try {
          collectionKind = CollectionKind.valueOf(collectionKindS);
          } catch (Exception e){
            // Ignore, means it is probably not a collection
          }
          if (collectionKind != null){
            String enclosingTypeName = fqTypeName.substring(parenthesisPos + 1, fqTypeName.length() - 1);
            // Return recursive call on enclosing type name
            return EdmCollectionType.newBuilder()
                .setKind(collectionKind).setCollectionType(resolveType(enclosingTypeName));
          }
        }
      }
      EdmType type = EdmType.getSimple(fqTypeName);
      EdmType.Builder<?, ?> builder = null;
      if (type != null) {
        builder = EdmSimpleType.newBuilder(type);
      } else {
        builder = findEdmEntityType(fqTypeName);
        if (builder == null) {
          builder = findEdmComplexType(fqTypeName);
        }
      }
      return builder;
    }

  }

}
