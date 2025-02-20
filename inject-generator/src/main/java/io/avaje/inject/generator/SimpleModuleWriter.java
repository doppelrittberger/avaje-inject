package io.avaje.inject.generator;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Write the source code for the factory.
 */
class SimpleModuleWriter {

  private static final String CODE_COMMENT_FACTORY =
    "/**\n" +
      " * Generated source - avaje inject module for %s.\n" +
      " * \n" +
      " * With Java module system this generated class should be explicitly\n" +
      " * registered in module-info via a <code>provides</code> clause like:\n" +
      " * \n" +
      " * <pre>{@code\n" +
      " * \n" +
      " *   module example {\n" +
      " *     requires io.avaje.inject;\n" +
      " *     \n" +
      " *     provides io.avaje.inject.spi.Module with %s.%s;\n" +
      " *     \n" +
      " *   }\n" +
      " * \n" +
      " * }</pre>\n" +
      " */";

  private static final String CODE_COMMENT_CREATE_CONTEXT =
    "  /**\n" +
      "   * Create the beans.\n" +
      "   * <p>\n" +
      "   * Creates all the beans in order based on constructor dependencies.\n" +
      "   * The beans are registered into the builder along with callbacks for\n" +
      "   * field injection, method injection and lifecycle support.\n" +
      "   */";

  private final ProcessingContext context;
  private final String modulePackage;
  private final String shortName;
  private final String fullName;
  private final ScopeInfo scopeInfo;
  private final MetaDataOrdering ordering;

  private Append writer;

  SimpleModuleWriter(MetaDataOrdering ordering, ProcessingContext context, ScopeInfo scopeInfo) {
    this.ordering = ordering;
    this.context = context;
    this.scopeInfo = scopeInfo;
    this.modulePackage = scopeInfo.modulePackage();
    this.shortName = scopeInfo.moduleShortName();
    this.fullName = scopeInfo.moduleFullName();
  }

  void write(ScopeInfo.Type scopeType) throws IOException {
    writer = new Append(createFileWriter());
    writePackage();
    writeStartClass();
    writeClassesMethod();
    writeBuildMethod();
    writeBuildMethods();
    writeEndClass();
    writer.close();
    if (scopeType != ScopeInfo.Type.CUSTOM) {
      writeServicesFile(scopeType);
    }
  }

  private void writeServicesFile(ScopeInfo.Type scopeType) {
    try {
      FileObject jfo = context.createMetaInfWriter(scopeType);
      if (jfo != null) {
        Writer writer = jfo.openWriter();
        writer.write(fullName);
        writer.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
      context.logError("Failed to write services file " + e.getMessage());
    }
  }

  private void writeClassesMethod() {
    Set<String> allClasses = distinctPublicClasses();
    writer.append("  @Override").eol();
    writer.append("  public Class<?>[] classes() {").eol();
    writer.append("    return new Class<?>[]{").eol();
    for (String rawType : allClasses) {
      writer.append("      %s.class,", rawType).eol();
    }
    writer.append("    };").eol();
    writer.append("  }").eol().eol();
  }

  /**
   * Return the distinct set of public classes that are dependency types.
   */
  private Set<String> distinctPublicClasses() {
    Set<String> publicClasses = new LinkedHashSet<>();
    for (MetaData metaData : ordering.ordered()) {
      String rawType = metaData.getType();
      if (!"void".equals(rawType)) {
        String type = GenericType.parse(rawType).topType();
        TypeElement element = context.element(type);
        if (element != null && element.getModifiers().contains(Modifier.PUBLIC)) {
          publicClasses.add(type);
        }
      }
    }
    return publicClasses;
  }

  private void writeBuildMethod() {
    writer.append(CODE_COMMENT_CREATE_CONTEXT).eol();
    writer.append("  @Override").eol();
    writer.append("  public void build(Builder builder) {").eol();
    if (scopeInfo.addWithBeans()) {
      writeWithBeans();
    }
    writer.append("    this.builder = builder;").eol();
    writer.append("    // create beans in order based on constructor dependencies").eol();
    writer.append("    // i.e. \"provides\" followed by \"dependsOn\"").eol();
    for (MetaData metaData : ordering.ordered()) {
      writer.append("    build_%s();", metaData.getBuildName()).eol();
    }
    writer.append("  }").eol();
    writer.eol();
  }

  private void writeBuildMethods() {
    for (MetaData metaData : ordering.ordered()) {
      writer.append(metaData.buildMethod(ordering)).eol();
    }
  }

  private void writePackage() {
    writer.append("package %s;", modulePackage).eol().eol();
    for (String type : factoryImportTypes()) {
      writer.append("import %s;", type).eol();
    }
    for (String type : scopeInfo.initModuleDependencies(ordering.importTypes())) {
      if (Util.validImportType(type)) {
        writer.append("import %s;", type).eol();
      }
    }
    writer.eol();
  }

  private Set<String> factoryImportTypes() {
    Set<String> importTypes = new TreeSet<>();
    importTypes.add(Constants.GENERATED);
    importTypes.add(Constants.BEANSCOPE);
    importTypes.add(Constants.INJECTMODULE);
    importTypes.add(Constants.DEPENDENCYMETA);
    importTypes.add(Constants.MODULE);
    importTypes.add(Constants.BUILDER);
    return importTypes;
  }

  private void writeStartClass() {
    writer.append(CODE_COMMENT_FACTORY, scopeInfo.name(), modulePackage, shortName).eol();
    scopeInfo.buildAtInjectModule(writer);

    String interfaceType = scopeInfo.type().type();
    writer.append("public class %s implements %s {", shortName, interfaceType).eol().eol();
    scopeInfo.buildFields(writer);
    if (scopeInfo.addModuleConstructor()) {
      writeConstructor();
    }
    writer.append("  @Override").eol();
    writer.append("  public Class<?>[] provides() {").eol();
    writer.append("    return provides;").eol();
    writer.append("  }").eol().eol();

    writer.append("  @Override").eol();
    writer.append("  public Class<?>[] requires() {").eol();
    writer.append("    return requires;").eol();
    writer.append("  }").eol().eol();
  }

  private void writeWithBeans() {
    writer.append("    // register external dependencies").eol();
    final Map<String, String> dependencies = scopeInfo.constructorDependencies();
    for (Map.Entry<String, String> entry : dependencies.entrySet()) {
      writer.append("    builder.withBean(%s.class, %s);", entry.getKey(), entry.getValue()).eol();
    }
  }

  private void writeConstructor() {
    final Map<String, String> dependencies = scopeInfo.constructorDependencies();
    for (Map.Entry<String, String> entry : dependencies.entrySet()) {
      writer.append("  private %s %s;", entry.getKey(), entry.getValue()).eol();
    }
    writer.eol();
    writer.append("  /**").eol();
    writer.append("   * Create providing the external dependencies.").eol();
    writer.append("   */").eol();
    writer.append("  public %s(", shortName);

    boolean comma = false;
    for (Map.Entry<String, String> entry : dependencies.entrySet()) {
      if (!comma) {
        comma = true;
      } else {
        writer.append(", ");
      }
      writer.append(entry.getKey()).append(" ").append(entry.getValue());
    }
    writer.append(") {", shortName).eol();
    for (Map.Entry<String, String> entry : dependencies.entrySet()) {
      writer.append("    this.%s = %s;", entry.getValue(), entry.getValue()).eol();
    }
    writer.append("  }").eol().eol();
  }

  private void writeEndClass() {
    writer.append("}").eol();
  }

  private Writer createFileWriter() throws IOException {
    return scopeInfo.moduleFile().openWriter();
  }

}
