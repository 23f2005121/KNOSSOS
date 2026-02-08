package de.tum.cit.fop.maze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generates random mazes for endless mode using a modified Prim's algorithm.
 * Creates a 33x33 grid where 1 = wall and 0 = walkable path.
 * The maze consists of 2x2 "rooms" connected by 2-wide corridors.
 * <p>
 * Algorithm Overview (Modified Prim's):
 * 1. Start with a grid completely filled with walls
 * 2. Pick a random starting cell and carve it out
 * 3. Add all neighboring cells to a "frontier" list
 * 4. Randomly pick a cell from the frontier
 * 5. Connect it to an already-visited neighbor by carving a passage
 * 6. Mark it as visited and add its unvisited neighbors to the frontier
 * 7. Repeat until all cells have been visited
 * <p>
 * Grid Structure:
 * - Logical size: 10x10 (number of "rooms")
 * - Each room is 3x3 blocks (2x2 path + 1 wall separator)
 * - 2-block thick outer walls for consistent borders
 * - Total physical size: 33x33 pixels
 */
public class MazeMap {

    // Physical grid dimensions (2 border + 10 rooms * 3 blocks + 2 border = 33)
    private static final int SIZE = 33;

    // Cell values
    private static final int WALL = 1;  // Solid wall tile
    private static final int PATH = 0;  // Walkable floor tile

    // Logical maze dimensions (number of rooms in each direction)
    private static final int LOGICAL_SIZE = 10;

    // Border thickness (top and left walls are 2 blocks thick)
    private static final int ROW_OFFSET = 2;  // Top border
    private static final int COL_OFFSET = 2;  // Left border

    // The generated maze grid (1 = wall, 0 = path)
    private int[][] grid;

    // Random number generator for maze generation
    private Random random;

    /**
     * Creates a new random maze.
     * Initializes the grid and immediately generates a maze using Prim's algorithm.
     */
    public MazeMap() {
        this.grid = new int[SIZE][SIZE];
        this.random = new Random();
        generateMaze();
    }

    /**
     * Gets the generated maze grid.
     * Used by MapManager to render and check collisions.
     *
     * @return 2D array where 1 = wall, 0 = path
     */
    public int[][] getMap() {
        return grid;
    }

    /**
     * Generates a random maze using a modified Prim's algorithm.
     * <p>
     * Steps:
     * 1. Fill the entire grid with walls
     * 2. Start at cell (0,0) and carve it out
     * 3. Add neighboring cells to the frontier list
     * 4. Repeatedly:
     *    - Pick a random frontier cell
     *    - Connect it to a random visited neighbor
     *    - Carve out the frontier cell
     *    - Add its unvisited neighbors to the frontier
     * 5. Continue until all cells are visited
     * <p>
     * This creates a perfect maze (exactly one path between any two points, no loops).
     */
    private void generateMaze() {
        // Step 1: Fill the entire grid with solid walls
        for (int r = 0; r < SIZE; r++) {
            Arrays.fill(grid[r], WALL);
        }

        // Initialize Prim's algorithm data structures
        boolean[][] visited = new boolean[LOGICAL_SIZE][LOGICAL_SIZE]; // Tracks processed rooms
        List<Point> frontier = new ArrayList<>();                      // "To-do" list of cells to process

        // Step 2: Pick the starting room (top-left corner at logical coordinates 0,0)
        Point start = new Point(0, 0);
        carveCell(start);                   // Physically dig out the room in the grid
        visited[start.r][start.c] = true;   // Mark it as visited
        addUnvisitedNeighborsToFrontier(start, visited, frontier); // Add adjacent rooms to frontier

        // Step 3: Process the frontier until the entire maze is carved
        while (!frontier.isEmpty()) {
            // Pick a random room from the frontier list
            int randIndex = random.nextInt(frontier.size());
            Point current = frontier.remove(randIndex);

            // Safety check: if this room was already processed, skip it
            if (visited[current.r][current.c]) {
                continue;
            }

            // Find an adjacent room that's already part of the maze
            Point visitedNeighbor = getRandomVisitedNeighbor(current, visited);

            if (visitedNeighbor != null) {
                // Connect the new room to the existing maze by carving a passage
                carvePassage(current, visitedNeighbor);

                // Carve out the new room itself
                carveCell(current);

                // Mark as visited and add its unvisited neighbors to the frontier
                visited[current.r][current.c] = true;
                addUnvisitedNeighborsToFrontier(current, visited, frontier);
            }
        }
        // Loop continues until the frontier is empty (all cells visited and carved)
    }

    /**
     * Carves out a 2x2 walkable room at the specified logical position.
     * Each logical cell occupies a 3x3 block in the physical grid:
     * - 2x2 path area
     * - 1-block wall separator on the right and bottom
     *
     * @param p the logical position of the room to carve
     */
    private void carveCell(Point p) {
        // Convert logical coordinates to physical grid coordinates
        int rStart = ROW_OFFSET + (p.r * 3);
        int cStart = COL_OFFSET + (p.c * 3);

        // Carve a 2x2 path area within the 3x3 block
        for (int r = rStart; r < rStart + 2; r++) {
            for (int c = cStart; c < cStart + 2; c++) {
                grid[r][c] = PATH;
            }
        }
    }

    /**
     * Connects two adjacent rooms by carving a passage through the wall between them.
     * Passages are 2 blocks wide to match the room width.
     * <p>
     * If rooms are vertically aligned: carves a 1-high, 2-wide horizontal passage
     * If rooms are horizontally aligned: carves a 2-high, 1-wide vertical passage
     *
     * @param p1 first room position (logical coordinates)
     * @param p2 second room position (logical coordinates, must be adjacent to p1)
     */
    private void carvePassage(Point p1, Point p2) {
        if (p1.r != p2.r) {
            // Vertical connection (rooms are stacked)
            // Find the wall row between them (the bottom of the higher room)
            int rWall = ROW_OFFSET + (Math.min(p1.r, p2.r) * 3) + 2;
            int cStart = COL_OFFSET + (p1.c * 3);

            // Carve a 1-high, 2-wide horizontal passage
            grid[rWall][cStart] = PATH;
            grid[rWall][cStart + 1] = PATH;
        } else {
            // Horizontal connection (rooms are side-by-side)
            // Find the wall column between them (the right side of the left room)
            int cWall = COL_OFFSET + (Math.min(p1.c, p2.c) * 3) + 2;
            int rStart = ROW_OFFSET + (p1.r * 3);

            // Carve a 2-high, 1-wide vertical passage
            grid[rStart][cWall] = PATH;
            grid[rStart + 1][cWall] = PATH;
        }
    }

    /**
     * Adds all unvisited neighbors of a cell to the frontier list.
     * Checks all four cardinal directions (north, south, east, west).
     * Only adds neighbors that are within bounds and haven't been visited yet.
     *
     * @param p        the cell to check neighbors for
     * @param visited  2D array tracking which cells have been visited
     * @param frontier the frontier list to add unvisited neighbors to
     */
    private void addUnvisitedNeighborsToFrontier(Point p, boolean[][] visited, List<Point> frontier) {
        // Cardinal directions: north, south, west, east
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] d : directions) {
            int nr = p.r + d[0]; // Neighbor row
            int nc = p.c + d[1]; // Neighbor column

            // If neighbor is in bounds and not yet visited, add to frontier
            if (isValid(nr, nc) && !visited[nr][nc]) {
                frontier.add(new Point(nr, nc));
            }
        }
    }

    /**
     * Finds a random visited neighbor of a frontier cell.
     * Used to determine which existing maze cell to connect a frontier cell to.
     * <p>
     * This is the key step that makes Prim's algorithm work:
     * - Frontier cells are always connected to the growing maze
     * - The choice of which visited neighbor to connect to is randomized
     * - This creates the maze's branching structure
     *
     * @param p       the frontier cell to check
     * @param visited 2D array tracking which cells have been visited
     * @return a random visited neighbor, or null if none exist
     */
    private Point getRandomVisitedNeighbor(Point p, boolean[][] visited) {
        ArrayList<Point> neighbors = new ArrayList<>();
        // Cardinal directions: north, south, west, east
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // Collect all visited neighbors
        for (int[] d : directions) {
            int nr = p.r + d[0];
            int nc = p.c + d[1];

            if (isValid(nr, nc) && visited[nr][nc]) {
                neighbors.add(new Point(nr, nc));
            }
        }

        // If no visited neighbors, return null
        if (neighbors.isEmpty()) {
            return null;
        }

        // Return a random visited neighbor
        return neighbors.get(random.nextInt(neighbors.size()));
    }

    /**
     * Checks if logical coordinates are within the maze bounds.
     *
     * @param r row in logical coordinates (0 to LOGICAL_SIZE-1)
     * @param c column in logical coordinates (0 to LOGICAL_SIZE-1)
     * @return true if the coordinates are valid, false otherwise
     */
    private boolean isValid(int r, int c) {
        return r >= 0 && r < LOGICAL_SIZE && c >= 0 && c < LOGICAL_SIZE;
    }

    /**
     * Simple container class for 2D coordinates in the logical maze grid.
     * Used instead of creating int arrays for better readability.
     */
    private static class Point {
        int r, c; // Row and column in logical coordinates

        /**
         * Creates a point at the specified logical coordinates.
         *
         * @param r row position
         * @param c column position
         */
        Point(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }
}
