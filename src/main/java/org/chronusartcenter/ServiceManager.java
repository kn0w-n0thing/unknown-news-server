package org.chronusartcenter;

public class ServiceManager {

    public enum SERVICE_TYPE {
        OSC_SERVICE,
    }
    private static ServiceManager INSTANCE;
    private Context context;

    private OscService oscService;

    private ServiceManager() {
        context = new Context();
    }

    public synchronized static ServiceManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ServiceManager();
        }
        return INSTANCE;
    }

    public synchronized Object getService(SERVICE_TYPE type) {
        switch (type) {
            case OSC_SERVICE:
                if (oscService == null) {
                    oscService = new OscService(context);
                }
                return oscService;
            default:
                return null;
        }
    }
}
