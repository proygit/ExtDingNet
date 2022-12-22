package IotDomain;


import SelfAdaptation.Instrumentation.MoteProbe;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing a gateway in the network.
 */
public class Gateway extends NetworkEntity implements Runnable {

    private LinkedList<MoteProbe> subscribedMoteProbes;
    private static Logger LOGGER = Logger.getLogger(Gateway.class.getName());

    public Thread getTask() {
        return task;
    }

    private  Thread task;
    private transient Timer timer;

    public static LinkedList<Mote> getSubscribedMotes() {
        return subscribedMotes;
    }

    public static void setSubscribedMotes(LinkedList<Mote> subscribedMotes) {
        Gateway.subscribedMotes = subscribedMotes;
    }

    private static LinkedList<Mote> subscribedMotes = new LinkedList<>();

    /**
     * A construtor creating a gateway with a given xPos, yPos, environment and transmission power.
     *
     * @param gatewayEUI        gateway identifier.
     * @param xPos              The x-coordinate of the gateway on the map.
     * @param yPos              The y-coordinate of the gateway on the map.
     * @param environment       The map of the environment.
     * @param transmissionPower The transmission power of the gateway.
     * @Effect creates a gateway with a given name, xPos, yPos, environment and transmission power.
     */
    public Gateway(Long gatewayEUI, Integer xPos, Integer yPos, Environment environment, Integer transmissionPower, Integer SF) {
        super(gatewayEUI, xPos, yPos, environment, transmissionPower, SF, 1.0);
        environment.addGateway(this);
        subscribedMoteProbes = new LinkedList<>();
        ApplicationServer.addSubscription(this);

    }

    /**
     * Returns the subscribed MoteProbes.
     *
     * @return The subscribed MoteProbes.
     */
    public LinkedList<MoteProbe> getSubscribedMoteProbes() {
        return subscribedMoteProbes;
    }

    public void addSubscription(MoteProbe moteProbe) {
        if (!getSubscribedMoteProbes().contains(moteProbe)) {
            subscribedMoteProbes.add(moteProbe);
        }
    }

    /**
     * Sends a received packet directly to the MQTT server.
     *
     * @param packet             The received packet.
     * @param senderEUI          The EUI of the sender
     * @param designatedReceiver The EUI designated receiver for the packet.
     */
    @Override
    protected void OnReceive(Byte[] packet, Long senderEUI, Long designatedReceiver) {
        getEnvironment().getMQTTServer().publish(new LinkedList<>(Arrays.asList(packet)), designatedReceiver, senderEUI, getEUI());
        for (MoteProbe moteProbe : getSubscribedMoteProbes()) {
            moteProbe.trigger(this, senderEUI);
        }


    }

    /**
     * Can be used in real world where Motes have Id(Internet ip address) and not String UID
     *
     * @param ipAddress
     * @throws UnknownHostException
     * @throws IOException
     */
    public void sendPingRequest(String ipAddress)
            throws UnknownHostException, IOException {
        InetAddress checkMoteIP = InetAddress.getByName(ipAddress);
        LOGGER.log(Level.INFO, "Send a ping to host" + ipAddress);
        if (checkMoteIP.isReachable(5000))
            LOGGER.log(Level.FINE, "Host is pinged");
        else
            LOGGER.log(Level.WARNING, "Host cant be reached");

    }
    /**
     * Below is a work-around to show the thread that can be started in an interval by the gateway to ping
     * the subscribed motes.
     */
    @Override
    public void run() {
        for(Mote mote : getSubscribedMotes()){
            try {
                Thread.sleep(1000);
                pingMotes(mote);
                LOGGER.log(Level.INFO, "Send a ping to Motes");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Host cant be reached");
                return;
            }


        }

        LOGGER.log(Level.FINE, "Ping finished");
        timer.cancel();
    }

    public void start() {
        task = new Thread(this);
        task.setName("gateway thread");
        timer = new Timer("timer", true);
        task.start();

    }

    public void pingMotes(Mote mote)  {
        LOGGER.log(Level.INFO, "Send a ping to Motes  :" + mote.getSensors().contains(MoteSensor.FAULTY));
        if(mote.getSensors().contains(MoteSensor.FAULTY)){
            ApplicationServer.takeAction(mote);


        }

    }
    public void informAppSer(Mote mote)  {
        LOGGER.log(Level.INFO, "Send a ping to Motes  :" + mote.getSensors().contains(MoteSensor.FAULTY));
        if(mote.getSensors().contains(MoteSensor.FAULTY)){
            ApplicationServer.takeAction(mote);


        }

    }
    public static void addSubscription(Mote mote) {
        if (!getSubscribedMotes().contains(mote)) {
            subscribedMotes.add(mote);
            setSubscribedMotes(subscribedMotes);

        }
    }
}
