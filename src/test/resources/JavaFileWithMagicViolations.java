import java.sql.PreparedStatement;

public class JavaFileWithMagicViolations {
  PreparedStatement preparedStatement;

  public JavaFileWithMagicViolations() {
    preparedStatement = new PreparedStatement();
    preparedStatement.setInt(1, 5);
    preparedStatement.setString(3, "Hey");
    preparedStatement.setString(4, "Hello");
  }

  public void modifyLiterals() {
    int val = 8;
    switch (val) {
      case 1:
        preparedStatement.setString(3, "Put");
        break;
      case 4:
        preparedStatement.setString(4, "Buy");
        break;
      case -2:
        preparedStatement.setString(7, "Bye");
        preparedStatement.setDouble(7, 4.56);
        break;
      default:
        preparedStatement.setInt(5, 9);
    }
  }

}