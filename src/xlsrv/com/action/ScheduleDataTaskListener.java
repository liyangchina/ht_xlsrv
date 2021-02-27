package xlsrv.com.action;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import xlsrv.com.action.ScheduleManager;

public class ScheduleDataTaskListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        new ScheduleManager();
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
