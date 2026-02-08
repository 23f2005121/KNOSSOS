package de.tum.cit.fop.maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pathfinding logic for enemies using the A* algorithm.
 * Calculates the shortest collision-free path from start to target on a grid.
 * <p>
 * A* uses three cost values to find the best path:
 * - G-Cost: Distance traveled from the start
 * - H-Cost: Estimated distance to target (Manhattan distance)
 * - F-Cost: Total cost (G + H)
 * <p>
 * The algorithm always picks the tile with the lowest F-Cost as the next step.
 */
public class AI {

    /**
     * Represents a single tile in the pathfinding grid.
     * Stores position, costs, and a reference to the previous tile in the path.
     */
    private static class Node {
        int x, y;           // Grid coordinates of this tile
        int gCost, hCost, fCost;  // Movement costs used by A*
        Node parent;        // The tile we came from to reach this one

        /**
         * Creates a new node at the given grid position.
         *
         * @param x horizontal grid coordinate
         * @param y vertical grid coordinate
         */
        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Calculates the shortest path from start to target using A* pathfinding.
     * Returns a list of coordinates the enemy should follow to reach the target.
     * Returns null if no valid path exists (blocked by walls or out of bounds).
     *
     * @param startX  starting horizontal coordinate of the enemy
     * @param startY  starting vertical coordinate of the enemy
     * @param targetX target horizontal coordinate (player or random wander point)
     * @param targetY target vertical coordinate
     * @param grid    2D array from the map file (0 = path, 1 = wall)
     * @return list of [x, y] coordinate pairs forming the path, or null if unreachable
     */
    public static List<int[]> findPath(int startX, int startY, int targetX, int targetY, int[][] grid) {
        // openList holds tiles we haven't visited yet but might explore
        ArrayList<Node> openList = new ArrayList<>();
        // closedList holds tiles we've already checked and don't need to revisit
        ArrayList<Node> closedList = new ArrayList<>();

        Node startNode = new Node(startX, startY);
        Node targetNode = new Node(targetX, targetY);
        openList.add(startNode); // Start with the enemy's current position

        // Keep searching while there are tiles left to explore
        while (!openList.isEmpty()) {
            // Find the node with the lowest F-Cost (most promising path)
            Node current = openList.get(0);
            for (Node n : openList) {
                if (n.fCost < current.fCost) current = n;
            }

            // Move current from open to closed (we're now checking it)
            openList.remove(current);
            closedList.add(current);

            // Check if we've reached the target
            if (current.x == targetNode.x && current.y == targetNode.y) {
                // Build the final path by backtracking through parent nodes
                List<int[]> path = new ArrayList<>();
                while (current.parent != null) {
                    path.add(new int[]{current.x, current.y});
                    current = current.parent; // Walk backwards to the start
                }
                Collections.reverse(path); // Flip it so it goes start -> target
                return path;
            }

            // Check all 4 neighboring tiles (up, down, left, right)
            int[][] neighbors = {{0,1}, {0,-1}, {1,0}, {-1,0}};
            for (int[] dir : neighbors) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                // Skip if out of bounds
                if (ny < 0 || ny >= grid.length || nx < 0 || nx >= grid[0].length) continue;
                // Skip if it's a wall
                if (grid[ny][nx] == 1) continue;
                // Skip if we've already processed this tile
                if (containsNode(closedList, nx, ny)) continue;

                // Calculate costs for this neighbor
                Node neighbor = new Node(nx, ny);
                neighbor.gCost = current.gCost + 1; // One step further from start
                neighbor.hCost = Math.abs(nx - targetNode.x) + Math.abs(ny - targetNode.y); // Manhattan distance
                neighbor.fCost = neighbor.gCost + neighbor.hCost; // Total cost
                neighbor.parent = current; // Remember where we came from

                // Add to open list if not already there
                if (!containsNode(openList, nx, ny)) {
                    openList.add(neighbor);
                }
            }
        }

        // If we've exhausted all tiles and didn't find the target, no path exists
        return null;
    }

    /**
     * Checks if a list of nodes contains a node at the given coordinates.
     * Helper method since Nodes are custom objects and can't use List.contains().
     *
     * @param list the list of nodes to search through
     * @param x    horizontal coordinate to check
     * @param y    vertical coordinate to check
     * @return true if a node with those coordinates exists in the list
     */
    private static boolean containsNode(List<Node> list, int x, int y) {
        for (Node n : list) if (n.x == x && n.y == y) return true;
        return false;
    }
}
