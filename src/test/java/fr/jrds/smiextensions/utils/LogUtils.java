package fr.jrds.smiextensions.utils;

import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;

import fr.jrds.smiextensions.log.LogAdapter;

public class LogUtils {

    public static LogAdapter setLevel(Class<?> clazz, LogLevel level, String... loggers) {
        LogFactory.setLogFactory(new ConsoleLogFactory());
        LogAdapter logger = LogAdapter.getLogger(clazz);
        logger.setLogLevel(level);
        for(String c: loggers) {
            LogFactory.getLogger(c).setLogLevel(level);
        }
        return logger;
    }

}
