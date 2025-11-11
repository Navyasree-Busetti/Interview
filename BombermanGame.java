import java.util.*;

class Position {
    int row, col;
    Position(int row, int col) { this.row = row; this.col = col; }
}

class Power {
    int type; // 1 = Range+1, 2 = Diagonal, 3 = Extra Bomb
    Position pos;
    Power(int type, Position pos) { this.type = type; this.pos = pos; }
}

class Bomb {
    Position pos;
    int range;
    boolean diagonalBlast;

    Bomb(Position pos, int range, boolean diagonalBlast) {
        this.pos = pos;
        this.range = range;
        this.diagonalBlast = diagonalBlast;
    }

    List<Position> blastPositions() {
        List<Position> list = new ArrayList<>();
        int[][] dirs = diagonalBlast
                ? new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}
                : new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs)
            for (int r = 1; r <= range; r++)
                list.add(new Position(pos.row + d[0]*r, pos.col + d[1]*r));
        return list;
    }
}

class Player {
    Position pos;
    int bombRange = 1;
    boolean diagonalBlast = false;
    int bombCount = 1;
    Player(Position pos) { this.pos = pos; }

    void collectPower(Power p) {
        if (p.type == 1) bombRange++;
        else if (p.type == 2) diagonalBlast = true;
        else bombCount++;
        System.out.println("Collected Power " + p.type);
    }
}

class GameMap {
    int size;
    char[][] grid;
    Player player;
    Position key;
    List<Position> villains = new ArrayList<>();
    List<Position> bricks = new ArrayList<>();
    List<Power> powers = new ArrayList<>();
    List<Bomb> bombs = new ArrayList<>();

    GameMap(int n) {
        this.size = n + 1; // +1 for alphabet border
        grid = new char[size][size];
        for (int i = 0; i < size; i++)
            Arrays.fill(grid[i], ' ');
        addBorders();
        addWalls();
    }

    void addBorders() {
        grid[0][0] = ' ';
        for (int j = 1; j < size; j++) grid[0][j] = (char)('A' + j - 1);
        for (int i = 1; i < size; i++) grid[i][0] = (char)('A' + i - 1);
    }

    void addWalls() {
        // Outer boundary walls
        for (int i = 1; i < size; i++) {
            grid[1][i] = '*';
            grid[size - 1][i] = '*';
            grid[i][1] = '*';
            grid[i][size - 1] = '*';
        }
        // Inner fixed walls
        for (int i = 3; i < size - 1; i += 2)
            for (int j = 3; j < size - 1; j += 2)
                grid[i][j] = '*';
    }

    boolean isOccupied(int r, int c) {
        return grid[r][c] != ' ';
    }

    boolean isWall(int r, int c) {
        return grid[r][c] == '*';
    }

    boolean canPlaceAt(Position p) {
        if (isWall(p.row, p.col)) {
            System.out.println("Can't place element on wall at " + toAlpha(p));
            return false;
        }
        if (isOccupied(p.row, p.col)) {
            System.out.println("Can't place element on occupied cell at " + toAlpha(p));
            return false;
        }
        return true;
    }

    void placePlayer(Player p) {
        if (!canPlaceAt(p.pos)) return;
        this.player = p;
        grid[p.pos.row][p.pos.col] = 'P';
    }

    void placeKey(Position k) {
        if (!canPlaceAt(k)) return;
        key = k;
        grid[k.row][k.col] = 'K';
    }

    void addVillain(Position v) {
        if (!canPlaceAt(v)) return;
        villains.add(v);
        grid[v.row][v.col] = 'V';
    }

    void addBrick(Position b) {
        if (!canPlaceAt(b)) return;
        bricks.add(b);
        grid[b.row][b.col] = 'B';
    }

    void addPower(Power p) {
        if (!canPlaceAt(p.pos)) return;
        powers.add(p);
        grid[p.pos.row][p.pos.col] = (char)('0' + p.type);
    }

    void printMap() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++)
                System.out.print(grid[i][j] + " ");
            System.out.println();
        }
        System.out.println();
    }

    boolean validMove(int r, int c) {
        return r > 1 && c > 1 && r < size - 1 && c < size - 1 &&
               grid[r][c] != '*' && grid[r][c] != 'B' && grid[r][c] != 'X';
    }

    void movePlayer(String dir) {
        int[][] mv = {
                {-1,0},{1,0},{0,-1},{0,1},   // W, S, A, D
                {-1,-1},{1,-1},{-1,1},{1,1} // Q, Z, E, C
        };
        char[] cmds = {'W','S','A','D','Q','Z','E','C'};
        int idx = -1;
        for (int i = 0; i < cmds.length; i++)
            if (cmds[i] == dir.toUpperCase().charAt(0)) idx = i;
        if (idx == -1) return;

        int nr = player.pos.row + mv[idx][0];
        int nc = player.pos.col + mv[idx][1];
        if (!validMove(nr, nc)) {
            System.out.println("Invalid move");
            return;
        }

        char dest = grid[nr][nc];
        if (dest == 'V') { System.out.println("Player Died!"); System.exit(0); }
        if (dest == 'K') { System.out.println("🎉 You Won!"); System.exit(0); }

        // Check power pickup
        for (Iterator<Power> it = powers.iterator(); it.hasNext();) {
            Power p = it.next();
            if (p.pos.row == nr && p.pos.col == nc) {
                player.collectPower(p);
                it.remove();
                break;
            }
        }

        // 🧨 Don't overwrite bomb when moving away
        if (grid[player.pos.row][player.pos.col] != 'X')
            grid[player.pos.row][player.pos.col] = ' ';

        player.pos = new Position(nr, nc);
        grid[nr][nc] = 'P';
        printMap();
    }

    void plantBomb() {
        if (bombs.size() >= player.bombCount) {
            System.out.println("Can't plant more bombs!");
            return;
        }
        Bomb b = new Bomb(new Position(player.pos.row, player.pos.col),
                          player.bombRange, player.diagonalBlast);
        bombs.add(b);
        grid[player.pos.row][player.pos.col] = 'X';
        System.out.println("Bomb planted at " + toAlpha(player.pos));
        printMap();
    }

    void detonateBombs() {
        if (bombs.isEmpty()) {
            System.out.println("No bomb planted!");
            return;
        }
        for (Bomb b : bombs) {
            for (Position p : b.blastPositions()) {
                if (p.row <= 0 || p.col <= 0 || p.row >= size || p.col >= size)
                    continue;

                char c = grid[p.row][p.col];
                // ❌ Skip walls and bombs
                if (c == '*' || c == 'X' || c == 'K') continue;
                // ✅ Destroy destructible items
                if (c == 'B' || c == 'V' || c == '1' || c == '2' || c == '3')
                    grid[p.row][p.col] = ' ';
                if (c == 'P') {
                    System.out.println("💥 Player Died in Blast!");
                    System.exit(0);
                }
            }
        }
        bombs.clear();
        printMap();
    }

    String toAlpha(Position p) {
        return "" + (char)('A' + p.row - 1) + (char)('A' + p.col - 1);
    }
}

public class BombermanGame {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        GameMap map = new GameMap(12);

        // --- Example setup ---
        Player p = new Player(new Position(3, 2)); // CB
        map.placePlayer(p);
        map.placeKey(new Position(6, 4));          // FD
        map.addVillain(new Position(2, 8));        // BH
        map.addBrick(new Position(4, 4));          // DD
        map.addBrick(new Position(10, 10));        // JJ
        map.addBrick(new Position(6, 7));          // FG
        map.addBrick(new Position(8, 3));          // HC

        // Powers (1-range+, 2-diagonal, 3-bomb+)
        map.addPower(new Power(1, new Position(5, 2))); // EB
        map.addPower(new Power(2, new Position(4, 3))); // DC
        map.addPower(new Power(3, new Position(5, 4))); // ED

        map.printMap();

        while (true) {
            System.out.print("Enter move (WASD/QZEC/X): ");
            String in = sc.next();
            if (in.equalsIgnoreCase("X")) {
                System.out.print("1-Plant  2-Detonate: ");
                int op = sc.nextInt();
                if (op == 1) map.plantBomb();
                else map.detonateBombs();
            } else {
                map.movePlayer(in);
            }
        }
    }
}
