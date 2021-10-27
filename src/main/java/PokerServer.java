import DTO.Card;
import DTO.DecodingMessage;
import baseAlghoritms.Environ;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;

public class PokerServer extends Listener {
    private static Server server;

    private final int p;
    private int userStepCount = 0;
    private final int gamersNumber;
    private final ArrayList<Integer> cards = new ArrayList<>();
    private final ArrayList<Integer> users = new ArrayList<>();

    public static void main(String[] args) throws Exception{
        int tcpPort = 27960;
        server = new Server();
        server.bind(tcpPort);
        server.getKryo().register(Integer.class);
        server.getKryo().register(ArrayList.class);
        server.getKryo().register(Long.class);
        server.getKryo().register(DecodingMessage.class);
        server.getKryo().register(Boolean.class);
        server.start();
        server.addListener(new PokerServer(Integer.parseInt(args[0]), (args.length > 1? Integer.parseInt(args[1]): 52)));
    }

    private PokerServer(int gamers, int cardsCounter){
        gamersNumber = gamers;
        p = Environ.getInstance().getP();
        switch (cardsCounter){
            case 54: case 52:
                for(int i = 0; i < cardsCounter; i++){
                    cards.add(i + 2);
                }
                break;
            case 36:
                for(int i = 4; i < 36; i += 4)
                    for (int j = i; j % 13 != 0; j++)
                        cards.add(j + 2);
                break;
            case 32:
                for(int i = 5; i < 36; i += 5)
                    for (int j = i; j % 13 != 0; j++)
                        cards.add(j + 2);
                break;
            case 28:
                for(int i = 6; i < 36; i += 6)
                    for (int j = i; j % 13 != 0; j++)
                        cards.add(j + 2);
                break;
            default:
                throw new IllegalArgumentException("Incorrect size of pack");
        }
        System.out.println("Cards:");
        cards.forEach(c -> System.out.println(new Card(c)));
    }

    @Override
    public void disconnected(Connection connection) {
        users.removeIf(e -> e == connection.getID());
        userStepCount = 0;
    }

    @Override
    public void received(Connection connection, Object object) {
        if(object instanceof Boolean){
            if((boolean) object) {
                connection.sendTCP((long) p);
                users.add(connection.getID());
                if (users.size() == gamersNumber) {
                    connection.sendTCP(cards);
                }
            }
        } else if(object instanceof ArrayList) {
            System.out.println("User step count: " + userStepCount);
            if (userStepCount < users.size() - 1) {
                server.sendToTCP(users.get(userStepCount), object);
                userStepCount++;
            } else{
                ArrayList<Integer> nowCards = (ArrayList<Integer>) object;
                for(int ip: users){
                    server.sendToTCP(ip, nowCards.remove(0));
                    server.sendToTCP(ip, nowCards.remove(0));
                }
            }
        } else if(object instanceof DecodingMessage){
            int indexUser = users.indexOf(connection.getID()) + 1;
            server.sendToTCP(indexUser == users.size()? users.get(0): users.get(indexUser), object);
        }
    }
}
