package main.mazes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import algorithms.PathFindingAlgorithm;
import algorithms.datatypes.Point;
import algorithms.vertexanya.VertexAnyaMarkingV3;
import grid.GridGraph;
import main.analysis.MazeAnalysis;
import main.analysis.TwoPoint;
import main.graphgeneration.AutomataGenerator;
import main.graphgeneration.UpscaledMapGenerator;
import main.testgen.StartEndPointData;
import main.testgen.Stringifier;
import main.utility.Utility;
import uiandio.GraphImporter;

public class StoredTestMazes {
    
    public static MazeAndTestCases loadAutomataMaze(int sizeIndex, int resolutionIndex) {

        int sizeX, sizeY;
        switch (sizeIndex) {
            case 0: sizeX = sizeY = 2000; break;
            case 1: sizeX = sizeY = 3000; break;
            case 2: sizeX = sizeY = 4000; break;
            case 3: sizeX = sizeY = 5000; break;
            case 4: sizeX = sizeY = 6000; break;
            case 5: sizeX = sizeY = 7000; break;
            case 6: sizeX = sizeY = 8000; break;
            default: throw new UnsupportedOperationException("Invalid sizeIndex: " + sizeIndex);
        }

        float resolution;
        switch (resolutionIndex) {
            case 0: resolution = 0.05f; break;
            case 1: resolution = 0.1f; break;
            case 2: resolution = 0.2f; break;
            case 3: resolution = 0.3f; break;
            case 4: resolution = 0.5f; break;
            case 5: resolution = 1f; break;
            case 6: resolution = 2f; break;
            case 7: resolution = 3f; break;
            case 8: resolution = 4f; break;
            case 9: resolution = 5f; break;
            default: throw new UnsupportedOperationException("Invalid resolutionIndex: " + resolutionIndex);
        }
        // Standardise resolution.
        resolution = resolution * 1000 / sizeX;

        int seed = sizeIndex + 577*(resolutionIndex+1);
        int problemSeed = resolutionIndex + 9127*(sizeIndex+1);

        int unblockedRatio = 5;
        int iterations = 3;
        int cutoffOffset = 0;
        boolean bordersAreBlocked = false;
        int nProblems = 100;

        GridGraph gridGraph = AutomataGenerator.generateSeededGraphOnly(seed, sizeX, sizeY, unblockedRatio, iterations, resolution, cutoffOffset, bordersAreBlocked);
        ArrayList<StartEndPointData> problems = generateProblems(gridGraph, nProblems, problemSeed); 
        String mazeName = Stringifier.automataToString(seed, sizeX, sizeY, unblockedRatio, iterations, resolution, cutoffOffset, bordersAreBlocked);
        return new MazeAndTestCases(mazeName, gridGraph, problems);
    }
    

    public static MazeAndTestCases loadScaledMaze(String mazeName, int multiplier) {
        ArrayList<StartEndPointData> problems = GraphImporter.loadStoredMazeProblemData(mazeName);
        GridGraph gridGraph = GraphImporter.loadStoredMaze(mazeName);

        GridGraph newGridGraph = UpscaledMapGenerator.upscale(gridGraph, multiplier, true);
        ArrayList<StartEndPointData> scaledProblems = new ArrayList<>();
        for (StartEndPointData p : problems) {
            int sx = p.start.x*multiplier;
            int sy = p.start.y*multiplier;
            int ex = p.end.x*multiplier;
            int ey = p.end.y*multiplier;
            
            scaledProblems.add(computeStartEndPointData(gridGraph, new Point(sx,sy), new Point(ex,ey)));
        }
        
        String newMazeName = mazeName + "_x" + multiplier;
        return new MazeAndTestCases(newMazeName, newGridGraph, scaledProblems);
    }
    
    
    private static ArrayList<StartEndPointData> generateProblems(GridGraph gridGraph, int nProblems, int seed) {
        ArrayList<ArrayList<Point>> connectedSets = MazeAnalysis.findConnectedSetsFast(gridGraph);
        Random rand = new Random(seed);
        ArrayList<StartEndPointData> problemList = new ArrayList<>();
        HashSet<TwoPoint> chosenProblems = new HashSet<>();
        
        int chances = nProblems; // prevent infinite loop        
        for (int i=0; i<nProblems; i++) {
            TwoPoint tp = generateProblem(rand, connectedSets);
            while (chances > 0 && chosenProblems.contains(tp)) {
                tp = generateProblem(rand, connectedSets);
                --chances;
            }
            chosenProblems.add(tp);
            
            problemList.add(computeStartEndPointData(gridGraph, tp.p1, tp.p2));
        }
        
        return problemList;
    }


    private static StartEndPointData computeStartEndPointData(GridGraph gridGraph, Point p1, Point p2) {
        PathFindingAlgorithm algo = new VertexAnyaMarkingV3(gridGraph, p1.x, p1.y, p2.x, p2.y);
        algo.computePath();
        int[][] path = algo.getPath();
        double shortestPathLength = Utility.computePathLength(gridGraph, path);
        return new StartEndPointData(p1, p2, shortestPathLength);
    }
    
    private static TwoPoint generateProblem(Random rand, ArrayList<ArrayList<Point>> connectedSets) {
        int nTraversableNodes = 0;
        for (ArrayList<Point> list : connectedSets) {
            nTraversableNodes += list.size();
        }
        int index = rand.nextInt(nTraversableNodes);
        int listIndex = 0;
        for (int i=0; i< connectedSets.size(); i++) {
            int size = connectedSets.get(i).size();
            if (index >= size) {
                index -= size;
            } else {
                listIndex = i;
                break;
            }
        }
        
        ArrayList<Point> list = connectedSets.get(listIndex);
        int index2 = rand.nextInt(list.size()-1);
        if (index2 == index) {
            index2 = list.size()-1;
        }
        
        Point p1 = list.get(index);
        Point p2 = list.get(index2);
        return new TwoPoint(p1, p2);
    }
}