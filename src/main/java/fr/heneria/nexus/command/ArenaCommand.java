package fr.heneria.nexus.command;

import fr.heneria.nexus.arena.manager.ArenaManager;
import fr.heneria.nexus.arena.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection; // <-- IMPORT AJOUTÉ
import java.util.Map;       // <-- IMPORT AJOUTÉ
import java.util.Optional;

public class ArenaCommand implements CommandExecutor {

    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /nx <arena|game|...> ...");
            return true;
        }

        String subCommandGroup = args[0].toLowerCase();
        if ("arena".equals(subCommandGroup)) {
            handleArenaCommand(sender, args);
        } else {
            sender.sendMessage(ChatColor.RED + "Commande inconnue. Usage: /nx arena ...");
        }

        return true;
    }

    private void handleArenaCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /nx arena <create|list|setspawn|save|delete>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                handleCreate(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "setspawn":
                handleSetSpawn(sender, args);
                break;
            case "save":
                handleSave(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Action d'arène inconnue: " + action);
                break;
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /nx arena create <nom> <maxJoueurs>");
            return;
        }
        String name = args[2];
        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Le nombre maximum de joueurs doit être un entier.");
            return;
        }

        Arena arena = arenaManager.createArena(name, maxPlayers);
        if (arena != null) {
            sender.sendMessage(ChatColor.GREEN + "L'arène '" + name + "' a été créée en mémoire.");
            sender.sendMessage(ChatColor.YELLOW + "Utilisez /nx arena setspawn pour définir les points de spawn, puis /nx arena save pour la sauvegarder.");
        } else {
            sender.sendMessage(ChatColor.RED + "Une arène avec le nom '" + name + "' existe déjà.");
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Liste des Arènes Chargées ---");
        Collection<Arena> arenas = arenaManager.getAllArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Aucune arène n'est chargée.");
        } else {
            for (Arena arena : arenas) {
                sender.sendMessage(ChatColor.YELLOW + "- " + arena.getName() +
                                   " (ID: " + arena.getId() +
                                   ", Max Joueurs: " + arena.getMaxPlayers() +
                                   ", Spawns: " + arena.getSpawns().values().stream().mapToLong(Map::size).sum() + ")");
            }
        }
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être exécutée que par un joueur.");
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /nx arena setspawn <nomArène> <équipe> <numéroSpawn>");
            return;
        }

        Player player = (Player) sender;
        String arenaName = args[2];
        int teamId, spawnNumber;
        try {
            teamId = Integer.parseInt(args[3]);
            spawnNumber = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "L'ID de l'équipe et le numéro du spawn doivent être des entiers.");
            return;
        }

        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            arena.setSpawn(teamId, spawnNumber, player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Spawn " + spawnNumber + " pour l'équipe " + teamId + " défini pour l'arène '" + arenaName + "'.");
            sender.sendMessage(ChatColor.YELLOW + "N'oubliez pas de sauvegarder l'arène avec /nx arena save " + arenaName);
        } else {
            sender.sendMessage(ChatColor.RED + "L'arène '" + arenaName + "' n'a pas été trouvée.");
        }
    }

    private void handleSave(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /nx arena save <nomArène>");
            return;
        }
        String arenaName = args[2];
        Optional<Arena> arenaOpt = arenaManager.getArena(arenaName);

        if (arenaOpt.isPresent()) {
            arenaManager.saveArena(arenaOpt.get());
            sender.sendMessage(ChatColor.GREEN + "L'arène '" + arenaName + "' et ses spawns ont été sauvegardés dans la base de données.");
        } else {
            sender.sendMessage(ChatColor.RED + "L'arène '" + arenaName + "' n'a pas été trouvée en mémoire.");
        }
    }
}
