package fr.jrds.smiextensions.log;

import java.util.Iterator;

import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;

public class LogAdapter {

    public static LogAdapter getLogger(Class<?> c) {
        return new LogAdapter(c);
    }

    private final org.snmp4j.log.LogAdapter adapter;

    private LogAdapter(Class<?> c) {
        adapter = LogFactory.getLogger(c);
    }

    public boolean isDebugEnabled() {
        return adapter.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return adapter.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return adapter.isWarnEnabled();
    }

    public void debug(String format, Object... args) {
        adapter.debug(LogString.make(format, args));
    }

    public void info(String format, Object... args) {
        adapter.info(LogString.make(format, args));
    }

    public void warn(String format, Object... args) {
        adapter.warn(LogString.make(format, args));
    }

    public void error(String format, Object... args) {
        adapter.error(LogString.make(format, args));
    }

    public void fatal(String format, Object... args) {
        adapter.fatal(LogString.make(format, args));
    }

    public void error(Throwable throwable, String format, Object... args) {
        adapter.error(LogString.make(format, args), throwable);
    }

    public void fatal(Throwable throwable, String format, Object... args) {
        adapter.fatal(LogString.make(format, args), throwable);
    }

    public void setLogLevel(LogLevel level) {
        adapter.setLogLevel(level);
    }

    public LogLevel getLogLevel() {
        return adapter.getLogLevel();
    }

    public LogLevel getEffectiveLogLevel() {
        return adapter.getEffectiveLogLevel();
    }

    public String getName() {
        return adapter.getName();
    }

    public Iterator<?> getLogHandler() {
        return adapter.getLogHandler();
    }

}
