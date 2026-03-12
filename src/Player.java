public class Player {

    private String playerName;
    private int money = 200;

    public Player(String playerName) {
        this.playerName = playerName;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }
}
