# code-violations
A naive duplicated string literals remover. 

May be used for the legacy projects where duplicate literals violations are present in enormous amount, 
and it's barely feasible to fix them in considerable amount of time.
 
Implementation is based on 
1. [Intellij Community edition](https://github.com/JetBrains/intellij-community) for constant generation  based on literal
2. [Java Sonar Checker](https://github.com/SonarSource/sonar-java) for mining files for violations

# Example 

Java file with multiple violations

```java
import java.util.ArrayList;

public class JavaFileWithDupeLiteralsViolations {
  private List<String> literals;

  public JavaFileWithDupeLiteralsViolations() {
    literals = new ArrayList<String>();
    literals.add("literal1");
    literals.add("literal1");
    literals.add("literal1"); literals.add("literal2");
    literals.add("literal2"); literals.add("literal3"); literals.add("literal1");
    literals.add("literal1");
  }

  public void modifyLiterals() {
    literals.remove("literal1");
    literals.remove("literal1");
    literals.remove("literal2");
    literals.remove("literal2");
  }

  public static class InnerClassWithLiteral{
    public static String LITERAL = "literal1";
  }
}
```

Java file after tool run

```java
import java.util.ArrayList;

public class JavaFileWithDupeLiteralsViolations {
public static final String LITERAL1 = "literal1";
public static final String LITERAL2 = "literal2";
  private List<String> literals;

  public JavaFileWithDupeLiteralsViolations() {
    literals = new ArrayList<String>();
    literals.add(LITERAL1);
    literals.add(LITERAL1);
    literals.add(LITERAL1); literals.add(LITERAL2);
    literals.add(LITERAL2); literals.add("literal3"); literals.add(LITERAL1);
    literals.add(LITERAL1);
  }

  public void modifyLiterals() {
    literals.remove(LITERAL1);
    literals.remove(LITERAL1);
    literals.remove(LITERAL2);
    literals.remove(LITERAL2);
  }

  public static class InnerClassWithLiteral{
    public static String LITERAL = LITERAL1;
  }
}
```

The basic idea is to use Sonar Java Checker to find out all literals violations in the project for each file.
 Check will return the exact literal value and where it's been found (line and column). The tool then uses JntelliJ
 approach to generate constant name (IntelliJ shortcut Ctrl+Alt+C), replaces all found violations with constant name
 and finally adds constant declaration right after class declaration.
