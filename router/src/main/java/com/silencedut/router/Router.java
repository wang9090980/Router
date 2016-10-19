package com.silencedut.router;

import com.silencedut.router.dispatcher.Dispatcher;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by SilenceDut on 16/8/1.
 */

public class Router {

    private Map<Class<?>,ReceiverHandler> mReceiverHandlerByInterface = new ConcurrentHashMap<>();

    //private Set<Object> mAllReceivers = new CopyOnWriteArraySet<>();

    private WeakHashMap<Class<?>,Object> mReceiversByType = new WeakHashMap<>();

    private Set<Dispatcher> mDispatchers = new HashSet<>();

    boolean mAnnotateMethodOnInterface;

    private Router() {}

    private static class InstanceHolder {
        private static Router sInstance = new Router();
    }

    public static Router getInstance() {
        return InstanceHolder.sInstance;
    }

    public <T> T getReceiver(Class<T> interfaceType) {
        ReceiverHandler receiverHandler = mReceiverHandlerByInterface.get(interfaceType);

        if(!interfaceType.isInterface()) {
            throw new RouterException(String.format("receiverType must be a interface , " +
                    "%s is not a interface",interfaceType.getName()));
        }

        if(receiverHandler==null) {
            receiverHandler = new ReceiverHandler(this,interfaceType,mReceiversByType);
            mReceiverHandlerByInterface.put(interfaceType,receiverHandler);
        }

        return (T)receiverHandler.mReceiverProxy;
    }

    public void setmAnnotateMethodOnInterface(boolean mAnnotateMethodOnInterface) {
        this.mAnnotateMethodOnInterface = mAnnotateMethodOnInterface;
    }

    void addDispatch(Dispatcher dispatcher) {
        mDispatchers.add(dispatcher);
    }



    public  void register(Object receiver) {
        if(receiver==null) {
            return;
        }
       mReceiversByType.put(receiver.getClass(),receiver);
    }

    public void unregister(Object receiver) {

        mReceiversByType.remove(receiver.getClass());

        Iterator iterator = mReceiverHandlerByInterface.keySet().iterator();
        while (iterator.hasNext()) {
            Class type = (Class) iterator.next();
            if(type.isInstance(receiver)&&
                    mReceiverHandlerByInterface.get(type).getSameTypeReceivesCount()==0) {
                iterator.remove();
            }
        }
        if(mReceiversByType.size()==0) {
            stopRouter();
        }
    }

    private void stopRouter() {
        for(Dispatcher dispatcher : mDispatchers) {
            dispatcher.stop();
        }
    }
}
