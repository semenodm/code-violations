import java.sql.PreparedStatement;

public class JavaFileWithMagicViolations {
public static final double _4_56 = 4.56;
public static final int _2 = 2;
public static final int _3 = 3;
public static final int _4 = 4;
public static final int _5 = 5;
public static final int _7 = 7;
public static final int _8 = 8;
public static final int _9 = 9;
  PreparedStatement preparedStatement;

  public JavaFileWithMagicViolations() {
    preparedStatement = new PreparedStatement();
    preparedStatement.setInt(1, _5);
    preparedStatement.setString(_3, "Hey");
    preparedStatement.setString(_4, "Hello");
  }

  public void modifyLiterals() {
    int val = _8;
    switch (val) {
      case 1:
        preparedStatement.setString(_3, "Put");
        break;
      case _4:
        preparedStatement.setString(_4, "Buy");
        break;
      case -_2:
        preparedStatement.setString(_7, "Bye");
        preparedStatement.setDouble(_7, _4_56);
        break;
      default:
        preparedStatement.setInt(_5, _9);
    }
  }

}