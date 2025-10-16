package huytq.example;

import java.util.logging.Logger;

public class ViolationOfEncapsulationExample {

    class User {
        private static final Logger logger = Logger.getLogger(User.class.getName());
        private String name;
        private int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void display() {
            // Dùng built-in formatting
            logger.info(() -> String.format("Name: %s, Age: %d", name, age));
        }
    }

    public static void main(String[] args) {
        ViolationOfEncapsulationExample example = new ViolationOfEncapsulationExample();
        User user = example.new User("Huy", 20);
        user.display();
    }
}
