import java.util.ArrayList;

public class JavaFileWithDupeLiteralsViolations {
  private List<String> literals;

  public JavaFileWithDupeLiteralsViolations() {
    literals = new ArrayList<String>();
    literals.add("literal1");
    literals.add("literal1");
    literals.add("literal1"); literals.add("literal2");
    literals.add("literal2"); literals.add("literal3"); literals.add("literal1");
    literals.add("literal1"); literals.add("UTF-8");
  }

  public void modifyLiterals() {
    literals.remove("literal1");
    literals.remove("literal1");
    literals.add("UTF-8");
    literals.remove("literal2");
    literals.remove("literal2");
    literals.remove("UTF-8");
  }

  public static class InnerClassWithLiteral{
    public static String LITERAL = "literal1";
  }
}