package util;

import java.io.*;
import java.util.*;
import java.lang.*;

public class CoReA{

    private String name;
    private String tie_scheme; // CS/CI, 1/CI, random
    private String selection_scheme; // CI/CS, random
    private int degeneracy_option; // 1,0
    private String ranking_scheme; // CoReA, CI, random
    private int budget;
    private Queue<Integer> deletion_order;
    
    private HashMap<Integer, Set<Integer>> incidence;
    private HashMap<Integer, int[]> hyperedges;
    private HashMap<Integer, Set<Integer>> anchors;
    private float[] size_distribution;

    private Set<Integer> nodes;
    private int[] degree;
    private int[] core_number;
    private int[] core_strength;
    private float[] core_influence;
    private float[] chance_cs_ci;
    private float[] chance_ci_cs;
    private float[] chance_one_ci;
    private float[] chance_random; // equal probability for all

    private int[] anchor_availability;
    private int max_node_id;

    // HashMap: key/value is the exact node id/hyperedge id
    // Array: array index is node id/hyperedge id - 1


    public CoReA(String name, int max_id, String tie, String scheme, int degeneracy, String rank, int budget) throws IOException
    {
        this.name = name;
        this.max_node_id = max_id;
        this.tie_scheme = tie;
        this.selection_scheme = scheme;
        this.degeneracy_option = degeneracy;
        this.ranking_scheme = rank;
        this.budget = budget;
       
        this.core_number = this.load_core_number();
        this.core_strength = this.load_core_strength();
        this.core_influence = this.load_core_influene();

        this.chance_random = new float[max_node_id];
        Arrays.fill(this.chance_random, 1);

        this.size_distribution = this.load_size_distribution(this.name);
        this.incidence = this.load_hypergraph(this.name);
        this.anchor_availability = this.compute_anchor_availability(this.tie_scheme);
        


    }

    // sampling with probability: choose size elements from candidates, probabilities proportional to weights
    public int[] sample(Set<Integer> candidates, float[] weights, int size){
        int[] sampled = new int[size];

        for(int j=0; j<size;j++){
            float total_weight = 0;
            for(int i: candidates){
                total_weight += weights[i-1];
                }
            double value = Math.random() * total_weight, weight = 0;
            for(int i: candidates){
                weight += weights[i-1];
                if (value < weight){
                    sampled[j] = i;
                    candidates.remove(i);
                    break;
                    }
                }
            
        }
        return sampled;
    }

    public void print_incidence(int final_id){
        for(int node: this.incidence.keySet()){
            if (node < final_id){
                for (int hyperedge_index: this.incidence.get(node)){
                System.out.println(hyperedge_index);
                }
            }
        }
    }

    public HashMap<Integer, Set<Integer>>  load_hypergraph(String name) throws IOException
    {
       
        // incidence
        HashMap<Integer, Set<Integer>> incidence = new HashMap<>();
        String directory = String.format("Data/%s-unique-simplices.txt", name);
        File f = new File(directory);
        int hyperedge_index;
        hyperedge_index = 0;
        HashMap<Integer, int[]> hyperedges = new HashMap<>();
        HashMap<Integer, Set<Integer>> anchors = new HashMap<>();
        int[] degree = new int[this.max_node_id];

        // initialize degree to be zero
        Arrays.fill(degree, 0);
      
        BufferedReader br = new BufferedReader(new FileReader(f));

        // initialize the incidence Array List
        for(int j = 1; j < this.max_node_id + 1; j++){
            Set<Integer> incidence_node = new HashSet<Integer>();
            incidence.put(j, incidence_node);
        }

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] hyperedge = br_line.split(" ");
            //System.out.println(Arrays.toString(hyperedge));
            int hyperedge_size = hyperedge.length;
            
            int[] hyperedge_ = new int[hyperedge_size];
            
            // parse and form hyperedge, compute hyperedge core number
            int hyperedge_core_number = this.core_number[Integer.valueOf(hyperedge[0])-1];
            for(int index=0; index<hyperedge_size; index++){
                hyperedge_[index] = Integer.valueOf(hyperedge[index]);
                if (this.core_number[hyperedge_[index] - 1] < hyperedge_core_number){
                    hyperedge_core_number = this.core_number[hyperedge_[index]-1];
                }
            }


            // store the hyperedge
            hyperedges.put(hyperedge_index, hyperedge_);

            // store incidence & update degree
            Set<Integer> anchor_list = new HashSet<Integer>();
            for(int index=0; index<hyperedge_size; index++){
                int node = hyperedge_[index];
                
                degree[node-1]++;

                if (this.core_number[node-1] == hyperedge_core_number){
                    anchor_list.add(node);
                }


                // update incidence
                if(incidence.containsKey(node)){
                    Set<Integer> incidence_list = incidence.get(node);
                    incidence_list.add(hyperedge_index);
                    incidence.put(node, incidence_list);
                }
                else{
                    Set<Integer> incidence_list = new HashSet<Integer>();
                    incidence_list.add(hyperedge_index);
                    incidence.put(node, incidence_list);
                }
                
            }
            anchors.put(hyperedge_index, anchor_list);
            hyperedge_index++;
        }
        br.close();
        String complete_string = String.format("done with incidence, degree, hyperedges, anchors of %s", name);
        System.out.println(complete_string);
        this.anchors = anchors;
        this.degree = degree;
        this.hyperedges = hyperedges;
        return incidence;
    }


    public static float[] load_size_distribution(String name) throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Size/%s-size-distribution.txt", name);
        File f = new File(directory);
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        float[] size_distribution = new float[24];

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] size_probability = br_line.split(" ");
            int size = Integer.valueOf(size_probability[0]);
            float probability = Float.valueOf(size_probability[1]);
            size_distribution[size - 2] = probability;
        }
        br.close();
        String complete_string = String.format("done with size distribution of %s", name);
        System.out.println(complete_string);
        
        return size_distribution;
        
    }

       public int[] load_core_number() throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Coreness/%s-coreness-rank.txt", name);
        File f = new File(directory);
        Set<Integer> nodes = new HashSet<Integer>();
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        int[] core_number = new int[this.max_node_id];
        Arrays.fill(core_number, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] cn = br_line.split(" ");
            int node = Integer.valueOf(cn[0]);
            int coreness = Integer.valueOf(cn[1]);
            core_number[node - 1] = coreness;
            nodes.add(node);
        }
        br.close();

        this.nodes = nodes;

        String complete_string = String.format("done with core numbers %s", name);
        System.out.println(complete_string);
        return core_number;
    }


    public int[] load_core_strength() throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Core Strength/%s-core-strength.txt", name);
        File f = new File(directory);
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        int[] core_strength = new int[this.max_node_id];
        Arrays.fill(core_strength, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] cs = br_line.split(" ");
            int node = Integer.valueOf(cs[0]);
            int strength = Integer.valueOf(cs[1]);
            core_strength[node - 1] = strength;
        }
        br.close();

        String complete_string = String.format("done with core strengths %s", name);
        System.out.println(complete_string);
        return core_strength;
    }

    public float[] load_core_influene() throws IOException
    {

        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Core Influence/%s-core-influence.txt", name);
        File f = new File(directory);
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        float[] core_influence = new float[this.max_node_id];
        float[] chance_cs_ci = new float[this.max_node_id];
        float[] chance_ci_cs = new float[this.max_node_id];
        float[] chance_one_ci = new float[this.max_node_id];

        Arrays.fill(core_influence, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] ci = br_line.split(" ");
            int node = Integer.valueOf(ci[0]);
            float influence = Float.valueOf(ci[1]);
            core_influence[node - 1] = influence;
            chance_cs_ci[node - 1] = this.core_strength[node-1] / core_influence[node-1];
            chance_ci_cs[node - 1] = 1/chance_cs_ci[node-1];
            chance_one_ci[node - 1] = 1/core_influence[node-1];
        }
        br.close();

        this.chance_cs_ci = chance_cs_ci;
        this.chance_ci_cs = chance_ci_cs;
        this.chance_one_ci = chance_one_ci;
        String complete_string = String.format("done with core influences %s", name);
        System.out.println(complete_string);
        return core_influence;

    }

    // core decomposition & compute anchor availabilities
    public int[] compute_anchor_availability(String tie_scheme) throws IOException
    {
        // anchor availabilities
        int[] anchor_availability = new int[this.max_node_id];

        // deletion queue
        Queue<Integer> deletion_order = new LinkedList<>();

        // copy all containers for easy computation
        HashMap<Integer, int[]> copy_hyperedges = new HashMap<Integer, int[]>(this.hyperedges);
        HashMap<Integer, Set<Integer>> copy_incidence = new HashMap<Integer, Set<Integer>>(this.incidence);
        int[] copy_degree = Arrays.copyOf(this.degree, this.degree.length);
        Set<Integer> copy_node = new HashSet<Integer>();
        copy_node.addAll(this.nodes);

        for(int k=1;;k++) {

            if(copy_node.isEmpty())
                break;
            Set<Integer> SDel = new HashSet<Integer>();
            for(int i : copy_node) {
                if(copy_degree[i-1] < k) {
                   SDel.add(i);
                }
            }

            // delete nodes of core numbers smaller than k
            while(!SDel.isEmpty()) {
                //final Set<Integer> SDelNew = new HashSet<Integer>();

                // tie-breaking scheme
                if (tie_scheme == "SI"){ // CS/CI
                    int[] chosen_node = this.sample(SDel, this.chance_cs_ci, 1);
                    int i = chosen_node[0];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }

                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

                else if (tie_scheme == "1I"){ // 1/CI
                    int[] chosen_node = this.sample(SDel, this.chance_one_ci, 1);
                    int i = chosen_node[0];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }
                    
                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

                else{ // random
                    Integer[] array_SDel = SDel.toArray(new Integer[SDel.size()]);
                    Random rndm = new Random();
                    int rndmNumber = rndm.nextInt(SDel.size());
                    int i = array_SDel[rndmNumber];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }
                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

            }
        }

        this.deletion_order = deletion_order;
        String complete_string = String.format("done with anchor availabilities of %s", name);
        System.out.println(complete_string);
        return anchor_availability;
    }

    // TODO: add attribute degeneracy, write Step 1-2 and Step 2.
}