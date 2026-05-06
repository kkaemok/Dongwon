package org.kkaemok.dongwon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kkaemok.dongwon.board.BoardService;

public final class BoardListener implements Listener {
    private final BoardService boardService;

    public BoardListener(BoardService boardService) {
        this.boardService = boardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        boardService.createBoard(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boardService.removeBoard(event.getPlayer());
    }
}
