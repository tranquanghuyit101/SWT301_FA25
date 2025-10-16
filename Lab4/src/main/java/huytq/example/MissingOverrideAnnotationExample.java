package huytq.example;

import java.util.logging.Logger;

class Animal {
    private static final Logger logger = Logger.getLogger(Animal.class.getName());

    void speak() {
        logger.info("Animal speaks");
    }
}

class Dog extends Animal {
    private static final Logger logger = Logger.getLogger(Dog.class.getName());

    @Override
    void speak() {  // đúng quy tắc đặt tên và override
        logger.info("Dog barks");
    }
}


