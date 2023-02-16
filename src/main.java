import java.io.*;
import java.util.*;

import util.CoReA;

public class main{


    public static void main(String[] args) throws IOException{

        String name = args[0];
        int max_node_id = Integer.parseInt(args[1]);
        String tie = args[2];
        int degeneracy = Integer.parseInt(args[3]);
        String scheme = args[4];
        String rank = args[5];
        int budget = Integer.parseInt(args[6]);
        int batch_size = Integer.parseInt(args[7]);

        System.out.println(rank);
        CoReA h_graph = new CoReA(name, max_node_id, tie, scheme, degeneracy, rank, budget, batch_size);
        HashMap<Integer, int[]> candidate_hyperedges = h_graph.construct_hyperedge(scheme, degeneracy);
        HashMap<Integer, int[]> augmented_hyperedges = h_graph.rank_select(candidate_hyperedges, rank, budget, batch_size);
        h_graph.write_results(augmented_hyperedges);
    }

}
