import java.util.Random;

public class Animal {
    private double row;
    private double col;

    private int targetRow;
    private int targetCol;
    private boolean isMoving = false;
    private final double SPEED = 1.5;
    private boolean isWaiting = false;
    private double waitTimer = 0.0;

    private double timeToProduce;
    private double currentTimeToProduce;
    private double timeToHungry;
    private double currentTimeToHungry;

    private boolean isHungry = false;
    private boolean hasProduct = false;

    private boolean facingRight = true;

    public Animal(int startRow, int startCol, double timeToProduce, double timeToHungry) {
        this.row = startRow;
        this.col = startCol;
        this.targetRow = startRow;
        this.targetCol = startCol;

        this.timeToHungry = timeToHungry;
        this.timeToProduce = timeToProduce;

        this.currentTimeToHungry = timeToHungry;
        this.currentTimeToProduce = timeToProduce;
    }

    public void Update(Soil[][] farm, int farmSize, double deltaTime, java.util.List<Animal> tousLesAnimaux) {
        if (isWaiting) {
            waitTimer -= deltaTime;

            if (waitTimer <= 0) {
                isWaiting = false;
                pickNewTarget(farm, farmSize, tousLesAnimaux);
                if (targetCol != col) {
                    facingRight = (targetCol > col);
                }
            }
        } else {
            double dx = targetRow - row;
            double dy = targetCol - col;
            double distance = Math.sqrt(dx * dx + dy * dy);

            double step = SPEED * deltaTime;

            if (distance > step) {
                row += (dx / distance) * step;
                col += (dy / distance) * step;
            } else {
                row = targetRow;
                col = targetCol;

                isWaiting = true;
                waitTimer = new Random().nextDouble(5.0 - 2.0 + 1.0) + 2.0;
            }
        }

        if (!isHungry && !hasProduct) {
            currentTimeToHungry -= deltaTime;
            if (currentTimeToHungry <= 0) {
                isHungry = true;
                System.out.println("Animal " + this.getClass().getSimpleName() + " a faim !");
            }
        }

        if (!isHungry && !hasProduct) {
            currentTimeToProduce -= deltaTime;
            if (currentTimeToProduce <= 0) {
                hasProduct = true;
                System.out.println("Animal " + this.getClass().getSimpleName() + " a produit quelque chose !");
            }
        }
    }

    private void pickNewTarget(Soil[][] farm, int farmSize, java.util.List<Animal> tousLesAnimaux) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        int randomStart = (int) (Math.random() * 4);

        for (int i = 0; i < 4; i++) {
            int[] dir = directions[(randomStart + i) % 4];
            int r = targetRow + dir[0];
            int c = targetCol + dir[1];

            if (r >= 0 && r < farmSize && c >= 0 && c < farmSize) {
                if (farm[r][c].isField() && !isTileOccupied(r, c, tousLesAnimaux)) {
                    targetRow = r;
                    targetCol = c;
                    break;
                }
            }
        }
    }

    public boolean isHit(double mouseX, double mouseY, double animalScreenX, double animalScreenY, double width, double height) {
        return mouseX >= animalScreenX - width / 2 &&
                mouseX <= animalScreenX + width / 2 &&
                mouseY >= animalScreenY - height &&
                mouseY <= animalScreenY;
    }


    public void feed() {
        if (isHungry) {
            isHungry = false;
            currentTimeToHungry = timeToHungry;
            System.out.println("Animal nourri !");
        }
    }

    public boolean collectProduct() {
        if (hasProduct) {
            hasProduct = false;
            currentTimeToProduce = timeToProduce;
            System.out.println("Produit récolté !");
            return true;
        }
        return false;
    }

    private boolean isTileOccupied(int r, int c, java.util.List<Animal> tousLesAnimaux) {
        for (Animal autreAnimal : tousLesAnimaux) {
            if (autreAnimal == this) continue;

            if (autreAnimal.targetRow == r && autreAnimal.targetCol == c) {
                return true;
            }
        }
        return false;
    }


    public boolean isHungry() { return isHungry; }
    public boolean hasProduct() { return hasProduct; }

    public double getRow() { return row; }
    public double getCol() { return col; }
    public int getTargetCol() { return targetCol; }
    public int getTargetRow() { return targetRow; }
    public boolean isFacingRight() { return facingRight; }
}