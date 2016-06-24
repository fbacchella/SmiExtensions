package fr.jrds.SMI4J.utils;

import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;

public class LogUtils {

    public static void setLevel(LogAdapter logger, LogLevel level, String... loggers) {
        LogFactory.setLogFactory(new ConsoleLogFactory());
        logger.setLogLevel(level);
        for(String c: loggers) {
            LogFactory.getLogger(c).setLogLevel(level);
        }
    }

}
