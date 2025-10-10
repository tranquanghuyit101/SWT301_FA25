package huytq.example;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AccountService {
    public static List<Account> accountList = new ArrayList<>();

    public boolean registerAccount(String username, String password, String email){
        //kiem tra username null hoac <= 3 ki tu
        if (username == null || !(username.length() > 3)){
            throw new IllegalArgumentException("Invalid username");
        }
        //kiem tra email hop le
        if (!isValidEmail(email)){
            throw new IllegalArgumentException("Invalid email address");
        }
        //kiem tra password hop le
        if (!isValidPassword(password)){
            throw new IllegalArgumentException("Invalid password");
        }
        //kiem tra trung username hoac email
        for (Account account : accountList) {
            if (account.getUsername().equals(username.trim())) {
                throw new IllegalArgumentException("Username already exists.");
            }
            if (account.getEmail().equals(email.trim())) {
                throw new IllegalArgumentException("Email already exists.");
            }
        }
        accountList.add(new Account(username, password, email));
        return true;
    }

    public boolean isValidEmail(String email){
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "(?:[a-z0-9!#$%&'*+\\x2f=?^_`\\x7b-\\x7d~\\x2d]+(?:\\.[a-z0-9!#$%&'*+\\x2f=?^_`\\x7b-\\x7d~\\x2d]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9\\x2d]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9\\x2d]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9\\x2d]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        return Pattern.matches(emailRegex, email);
    }

    public boolean isValidPassword(String password){
        //password null
        if(password == null || password.isEmpty()) return false;
        //password <= 6 ki tu
        if (!(password.length() > 6)) return false;
        //password khong co chu hoa
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        //password khong co so
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        //password khong co ki tu dac biet
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return false;
        }
        return true;
    }
}