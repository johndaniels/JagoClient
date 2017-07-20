package jagoclient;

import javax.swing.*;

/**
 * Created by johndaniels on 7/19/17.
 */
public class LoginDialog extends JDialog {
    JLabel passwordLabel;
    JLabel usernameLabel;
    JTextField usernameField;
    JPasswordField passwordField;
    JButton loginButton;
    JButton cancelButton;
    public interface LoginClicked  {
        void clicked(String username, String password);
    }
    LoginClicked loginClicked;
    LoginDialog(LoginClicked loginClicked) {
        this.loginClicked = loginClicked;
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.PAGE_AXIS));
        passwordLabel = new JLabel("Password");
        usernameLabel = new JLabel("Username");
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        rootPanel.add(usernameLabel);
        rootPanel.add(usernameField);
        rootPanel.add(passwordLabel);
        rootPanel.add(passwordField);

        JPanel buttonsPanel = new JPanel();
        loginButton = new JButton("Login");
        loginButton.addActionListener(event -> loginClicked.clicked(usernameField.getText(), new String(passwordField.getPassword())));
        cancelButton = new JButton("Cancel");
        buttonsPanel.add(loginButton);
        buttonsPanel.add(cancelButton);
        rootPanel.add(buttonsPanel);

        this.setContentPane(rootPanel);
    }
}
