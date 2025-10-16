package huytq.example;

import java.util.logging.Logger;

public class TightCouplingExample {

    interface Printer {
        void print(String message);
    }
    static class ConsolePrinter implements Printer {
        private static final Logger logger = Logger.getLogger(ConsolePrinter.class.getName());

        @Override
        public void print(String message) {
            logger.info(message); // dùng Logger thay cho System.out
        }
    }
    static class Report {
        private final Printer printer;

        // Dependency Injection: không khởi tạo trực tiếp Printer
        public Report(Printer printer) {
            this.printer = printer;
        }

        void generate() {
            printer.print("Generating report...");
        }
    }
    // Thêm hàm main để kiểm tra
    public static void main(String[] args) {
        Printer printer = new ConsolePrinter();
        Report report = new Report(printer);
        report.generate();
    }
}
