package project;

/*
        Author: Oleg Poliakov
        Start Time: 2024-11-07, Thu, 12:58
        End Time: 2024-11-12, Fri, 13:39
*/

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Main {

    public static final String[] COLOR_NAMES = {"blue", "red", "green", "orange", "magenta", "yellow"};
    public static final Color[] COLORS = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.YELLOW};

    public static String[] correct_sequence;
    public static int attempt;
    public static int attempt_limit;

    public static boolean is_game_over;


    static File game_folder = new File("./game");
    static File colors_folder = new File(game_folder.getPath() + "/colors");
    static File guess_folder = new File(game_folder.getPath() + "/guess");
    static File info_file = new File(game_folder.getPath() + "/info");
    static File check_file = new File(game_folder.getPath() + "/delete me to check");
    static File restart_file = new File(game_folder.getPath() + "/delete me to restart");


    public static Color get_color_by_name(String name) {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String color_name = COLOR_NAMES[i];
            if (color_name.equals(name)) {
                return COLORS[i];
            }
        }
        return Color.WHITE;
    }


    public static void empty_folder(File folder, boolean delete_folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    empty_folder(f, true);
                } else {
                    f.delete();
                }
            }
        }
        if (delete_folder) {
            folder.delete();
        }
    }

    public static void create_color_files() {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            String color = COLOR_NAMES[i];

            int order = 1;
            if (guess_folder.exists()) {
                order = guess_folder.listFiles().length + 1;
            }

            File color_file = new File(colors_folder.getPath() + "/" + order + color + ".png");

            if (!color_file.exists()) {

                for (File file : colors_folder.listFiles()) {
                    if (file.getName().contains(color)) {
                        file.delete();
                    }
                }

                if (order <= 4) {

                    BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    g.setColor(COLORS[i]);
                    g.fillRect(0, 0, 100, 100);
                    try {
                        ImageIO.write(image, "png", color_file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
    }

    public static void create_custom_color_files(String[] colors_array, String folder_path) {
        for (int i = 0; i < colors_array.length; i++) {
            String color = colors_array[i];

            File color_file = new File(folder_path + "/" + (i + 1) + color + ".png");

            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(get_color_by_name(color));
            g.fillRect(0, 0, 100, 100);
            try {
                ImageIO.write(image, "png", color_file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
    }


    public static void set_info(String name) {
        File rename = new File(game_folder.getPath() + "/" + name);
        if (!info_file.equals(rename)) {
            info_file.delete();
            info_file = rename;

            try {
                info_file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        while (true) {
            start_game(args);
        }
    }

    public static void start_game(String[] args) {

        is_game_over = false;

        attempt = 0;
        attempt_limit = 7;

        correct_sequence = new String[]{"", "", "", ""};

        for (int i = 0; i < 4; i++) {
            Random random = new Random();
            String rand_color = COLOR_NAMES[random.nextInt(COLOR_NAMES.length)];
            correct_sequence[i] = rand_color;
        }

        if(args.length>0 && args[0].equals("debug")) {
        System.out.println("CORRECT SEQUENCE: " + Arrays.toString(correct_sequence));
        }

        if (!game_folder.exists()) {
            if (game_folder.mkdir()) {
                System.out.println("Game folder didn't exist, created");
            }
        } else {
            System.out.println("Game folder existed, deleting content...");

            empty_folder(game_folder, false);

        }


        try {
            if (info_file.createNewFile()) {
                System.out.println("Created info file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (check_file.createNewFile()) {
                System.out.println("Created check file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        set_info("Place 4 colors from the \'colors\' folder into the \'guess\' folder");

        if (colors_folder.mkdir()) {
            System.out.println("Created colors folder");
        }

        System.out.println("Creating colors...");

        create_color_files();


        if (guess_folder.mkdir()) {
            System.out.println("guess folder created ");
        }

        System.out.println("Starting folders watch...");

        System.out.println("Go to \"" + game_folder.getAbsolutePath() + "\" to play the game");

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            WatchKey colors_folder_watch = colors_folder.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_CREATE);
            WatchKey guess_folder_watch = guess_folder.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_CREATE);
            game_folder.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_CREATE);


            WatchKey watchKey;

            while ((watchKey = watchService.take()) != null) {
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    Path changedPath = (Path) event.context();
                    if (changedPath != null) {
                        int number_of_colors = 0;
                        if (!is_game_over) {
                            number_of_colors = guess_folder.listFiles().length;


                            if (!changedPath.getFileName().equals(info_file.toPath().getFileName())) {

                                if (!info_file.exists()) {
                                    try {
                                        info_file.createNewFile();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }


                                if (number_of_colors > 0 && number_of_colors < 4) {
                                    set_info("you need " + (4 - number_of_colors) + " more");
                                } else if (number_of_colors > 4) {
                                    set_info("too much remove " + (number_of_colors - 4) + (((number_of_colors - 4) == 1) ? " color" : " colors"));
                                } else if (number_of_colors == 4) {
                                    set_info("correct number of colors");

                                }
                            }

                        }

                        if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                            if (!is_game_over) {
                                create_color_files();
                                if (changedPath.getFileName().equals(check_file.toPath().getFileName())) {

                                    if (number_of_colors == 4) {

                                        File[] guess_color_files = guess_folder.listFiles((dir, name) -> {
                                            boolean result = false;
                                            for (String color : COLOR_NAMES) {
                                                if (name.contains(color)) {
                                                    result = true;
                                                    break;
                                                }
                                            }
                                            return result;
                                        });


//                                        System.out.println("GUESS: " + Arrays.toString(guess_color_files));

                                        String info_text = "";

                                        String[] guessed_sequence = {"", "", "", ""};
                                        if (guess_color_files.length != 4) {
                                            set_info("ERROR incorrect color files");
                                        } else {
                                            HashMap<String, Integer> picked_colors = new HashMap<String, Integer>();

                                            for (String color : COLOR_NAMES) {
                                                picked_colors.put(color, 0);
                                            }
                                            for (int i = 0; i < 4; i++) {
                                                for (File guess_color_file : guess_color_files) {
                                                    String file_name = guess_color_file.getName();
                                                    if (file_name.startsWith(String.valueOf(i + 1))) {
                                                        String color = file_name.substring(1, file_name.length() - 4);

                                                        picked_colors.replace(color, picked_colors.get(color) + 1);
                                                        guessed_sequence[i] = color;
                                                    }
                                                }
                                            }

                                            int placed_correctly = 0;
                                            int correct = 0;
                                            for (int i = 0; i < 4; i++) {
                                                if (correct_sequence[i].equals(guessed_sequence[i])) {
                                                    placed_correctly++;
                                                }
                                            }

                                            for (String color : correct_sequence) {
                                                if (picked_colors.get(color) > 0) {
                                                    correct++;
                                                    picked_colors.replace(color, picked_colors.get(color) - 1);

                                                }
                                            }

                                            if (placed_correctly == 4) {
                                                info_text = "Correct you won!";
                                                guess_folder_watch.cancel();
                                                colors_folder_watch.cancel();
                                                empty_folder(guess_folder, true);
                                                empty_folder(colors_folder, true);
                                                check_file.delete();

                                                is_game_over = true;
                                                restart_file.createNewFile();


                                            }
                                            else {
                                                info_text = correct + " correct colors " + placed_correctly + " on the right spot";

                                            }

                                             if (attempt>=attempt_limit && !is_game_over){
                                                info_text = "You ran out of guesses game over ," + info_text;
                                                guess_folder_watch.cancel();
                                                colors_folder_watch.cancel();
                                                empty_folder(guess_folder, true);
                                                empty_folder(colors_folder, true);
                                                check_file.delete();

                                                is_game_over = true;
                                                restart_file.createNewFile();

                                                 File correct_sequence_folder = new File(game_folder.getPath() + "/correct sequence");

                                                 correct_sequence_folder.mkdir();

                                                 create_custom_color_files(correct_sequence, correct_sequence_folder.getPath());

                                            }

                                            set_info(info_text);

                                        }

                                        attempt++;


                                        File attempt_folder = new File(game_folder.getPath() + "/attempt" + attempt);
                                        attempt_folder.mkdir();

                                        File attempt_info_file = new File(attempt_folder.getPath() + "/result  " + info_text);
                                        attempt_info_file.createNewFile();

                                        create_custom_color_files(guessed_sequence, attempt_folder.getPath());

                                        empty_folder(guess_folder, false);

                                    }
                                    if (!is_game_over) {
                                        try {
                                            check_file.createNewFile();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }

                            } else {
                                if (changedPath.getFileName().equals(restart_file.toPath().getFileName())) {
                                    System.out.println("Restarting");
                                    return;
                                }
                            }
                        }
                    }
                }

                if (!watchKey.reset()) {
                    break;
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("finished");


    }
}