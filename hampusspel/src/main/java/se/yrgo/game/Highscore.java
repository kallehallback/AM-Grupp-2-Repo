package se.yrgo.game;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Highscore {

    public Map<String, Integer> loadScore(String fileName){
        Map<String, Integer> scores = new HashMap<>();
        Path path = Path.of("src\\main\\resources\\" + fileName);
        try(BufferedReader br = Files.newBufferedReader(path)){
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("///");
                String name = data[0];
                int score = Integer.parseInt(data[1]);
                Player player = new Player(name, score);
                scores.put(player.name, player.score);
            }
        }
        catch(FileNotFoundException e){
            System.out.println("Kan inte hitta fil!");
        }
        catch(IOException e){
            System.out.println("Kan inte hitta fil!");
        }

        scores = sortScores(scores);
        return scores;

    }
    public void saveScore(Map<String, Integer> scores, String fileName){
        Path path = Path.of("src\\main\\resources\\" + fileName);
        try(BufferedWriter writer = Files.newBufferedWriter(path)){
            scores.forEach((k, v)->{
                try {
                    writer.write(k + "///" + v);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch(FileNotFoundException e){
            System.out.println("Kan inte hitta fil!");
        }
        catch(IOException e){
            System.out.println("Kan inte hitta fil!");
        }
    }

    public Map<String, Integer> sortScores(Map<String, Integer> scores){
        Map<String, Integer> sortedScores = scores.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        return sortedScores;
    }
}
