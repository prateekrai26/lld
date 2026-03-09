    package logger;

    import java.util.HashSet;
    import java.util.Set;

    enum LOG_TYPE {
        INFO,DEBUG,ERROR
    }


    class Logger {

        LOG_TYPE type;
        Logger nextLogger;
        Set<Writer> writers;
        Logger(LOG_TYPE type, Logger nextLogger) {
            this.type = type;
            this.nextLogger = nextLogger;
            this.writers = new HashSet<>();
        }

        void addWriter(Writer writer) {
            writers.add(writer);
        }
        void removeWriter(Writer writer) {
            writers.remove(writer);
        }
        void log(LOG_TYPE logType, String message) {

            if (logType.ordinal() >= this.type.ordinal()) {
                for(Writer writer : writers) {
                    writer.write(message);
                }
            }

            if (nextLogger != null) {
                nextLogger.log(logType, message);
            }
        }
    }

    interface Writer {
        void write(String message);
    }

    class ConsoleWriter implements Writer {

        @Override
        public void write(String message) {
            System.out.println("writing to console");
            System.out.println(message);
        }
    }

    class FileWriter implements Writer {

        @Override
        public void write(String message) {
            System.out.println("writing to File");
            System.out.println(message);
        }
    }

    class ObjectStoreWriter implements Writer {

        @Override
        public void write(String message) {
            System.out.println("writing to ObjectStore");
            System.out.println(message);
        }
    }



    public class LoggingFramework {

        public static void main(String[] args) {

            Logger errorLogger = new Logger(LOG_TYPE.ERROR, null);
            Logger debugLogger = new Logger(LOG_TYPE.DEBUG, errorLogger);
            Logger logger = new Logger(LOG_TYPE.INFO, debugLogger);

            logger.addWriter(new ConsoleWriter());

            debugLogger.addWriter(new FileWriter());
            debugLogger.addWriter(new ObjectStoreWriter());

            errorLogger.addWriter(new FileWriter());
            errorLogger.addWriter(new ObjectStoreWriter());
            errorLogger.addWriter(new ConsoleWriter());

            logger.log(LOG_TYPE.INFO, "Hello World");

            logger.log(LOG_TYPE.DEBUG, "Debug World");

            logger.log(LOG_TYPE.ERROR, "Error happened");

        }
    }