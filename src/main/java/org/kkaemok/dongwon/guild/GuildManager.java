package org.kkaemok.dongwon.guild;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.kkaemok.dongwon.progression.ProfileManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class GuildManager {
    private static final String DEFAULT_GUILD_NAME = "미가입";
    private static final Pattern GUILD_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9가-힣_]{2,12}$");

    private final Plugin plugin;
    private final ProfileManager profileManager;
    private final File file;
    private final Map<String, Guild> guildsById = new HashMap<>();
    private final Map<String, String> guildIdByNormalizedName = new HashMap<>();
    private final Map<UUID, String> memberGuildId = new HashMap<>();
    private final Map<String, Set<UUID>> joinRequestsByGuildId = new HashMap<>();
    private final Map<UUID, GuildInvite> invitesByTarget = new HashMap<>();
    private final LuckPerms luckPerms;
    private YamlConfiguration config;

    public GuildManager(Plugin plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.luckPerms = findLuckPerms();
        load();
    }

    public boolean isAvailable() {
        return luckPerms != null;
    }

    public Result createGuild(Player leader, String rawGuildName, String rawColor) {
        if (!isAvailable()) {
            return fail("LuckPerms가 없어서 길드 시스템을 사용할 수 없습니다.");
        }
        if (isInGuild(leader.getUniqueId())) {
            return fail("이미 길드에 가입되어 있습니다.");
        }

        String guildName = rawGuildName == null ? "" : rawGuildName.trim();
        if (!GUILD_NAME_PATTERN.matcher(guildName).matches()) {
            return fail("길드명은 2~12자, 한글/영문/숫자/_ 만 사용할 수 있습니다.");
        }

        String normalizedName = normalizeGuildName(guildName);
        if (guildIdByNormalizedName.containsKey(normalizedName)) {
            return fail("같은 이름의 길드가 이미 존재합니다.");
        }

        Optional<String> parsedColor = GuildColor.parse(rawColor);
        if (parsedColor.isEmpty()) {
            return fail("색상 입력이 올바르지 않습니다. 예시: " + GuildColor.examples());
        }

        String guildId = UUID.randomUUID().toString();
        String lpGroupName = generateLuckPermsGroupName(guildName);
        Guild guild = new Guild(guildId, guildName, parsedColor.get(), leader.getUniqueId(), lpGroupName);

        try {
            Group group = luckPerms.getGroupManager().createAndLoadGroup(lpGroupName).join();
            group.data().add(SuffixNode.builder(guild.getSuffixValue(), 50).build());
            luckPerms.getGroupManager().saveGroup(group).join();
            addMemberInternal(guild, leader.getUniqueId());
        } catch (CompletionException | IllegalStateException ex) {
            plugin.getLogger().warning("길드 생성 중 LuckPerms 처리 실패: " + ex.getMessage());
            return fail("길드를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        guildsById.put(guild.getId(), guild);
        guildIdByNormalizedName.put(normalizedName, guild.getId());
        joinRequestsByGuildId.putIfAbsent(guild.getId(), new LinkedHashSet<>());
        save();
        return ok("길드 생성 완료: " + guild.getColoredName());
    }

    public Result deleteGuild(Player actor, String targetGuildName, boolean isAdmin) {
        Guild guild;
        if (targetGuildName != null && !targetGuildName.isBlank()) {
            if (!isAdmin) {
                return fail("다른 길드는 관리자만 삭제할 수 있습니다.");
            }
            guild = getGuildByName(targetGuildName).orElse(null);
            if (guild == null) {
                return fail("해당 길드를 찾을 수 없습니다.");
            }
        } else {
            guild = getGuildOf(actor.getUniqueId()).orElse(null);
            if (guild == null) {
                return fail("소속된 길드가 없습니다.");
            }
            if (!guild.getLeaderId().equals(actor.getUniqueId()) && !isAdmin) {
                return fail("길드 리더 또는 관리자만 길드를 삭제할 수 있습니다.");
            }
        }

        for (UUID memberId : new ArrayList<>(guild.getMembers())) {
            removeMemberInternal(guild, memberId);
        }

        joinRequestsByGuildId.remove(guild.getId());
        invitesByTarget.entrySet().removeIf(entry -> entry.getValue().guildId().equals(guild.getId()));
        guildsById.remove(guild.getId());
        guildIdByNormalizedName.remove(normalizeGuildName(guild.getName()));

        try {
            Group group = luckPerms.getGroupManager().getGroup(guild.getLuckPermsGroupName());
            if (group != null) {
                luckPerms.getGroupManager().deleteGroup(group).join();
            }
        } catch (CompletionException | IllegalStateException ex) {
            plugin.getLogger().warning("길드 그룹 삭제 실패: " + ex.getMessage());
        }

        save();
        return ok("길드가 삭제되었습니다: " + guild.getName());
    }

    public Result leaveGuild(Player player) {
        Guild guild = getGuildOf(player.getUniqueId()).orElse(null);
        if (guild == null) {
            return fail("소속된 길드가 없습니다.");
        }
        if (guild.getLeaderId().equals(player.getUniqueId())) {
            return fail("리더는 탈퇴할 수 없습니다. /길드 삭제 를 사용하세요.");
        }
        removeMemberInternal(guild, player.getUniqueId());
        save();
        return ok("길드에서 탈퇴했습니다: " + guild.getName());
    }

    public Result kickMember(Player leader, String targetName, boolean isAdmin) {
        Guild guild = getGuildOf(leader.getUniqueId()).orElse(null);
        if (guild == null) {
            return fail("소속된 길드가 없습니다.");
        }
        if (!guild.getLeaderId().equals(leader.getUniqueId()) && !isAdmin) {
            return fail("길드 리더 또는 관리자만 퇴출할 수 있습니다.");
        }
        UUID targetId = findGuildMemberByName(guild, targetName).orElse(null);
        if (targetId == null) {
            return fail("해당 길드원은 존재하지 않습니다.");
        }
        if (guild.getLeaderId().equals(targetId)) {
            return fail("리더는 퇴출할 수 없습니다.");
        }
        removeMemberInternal(guild, targetId);
        save();

        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            target.sendMessage("§c길드에서 퇴출되었습니다: " + guild.getName());
        }
        return ok("길드원 퇴출 완료.");
    }

    public Result adminSetGuildName(String targetGuildName, String newGuildName) {
        Guild guild = getGuildByName(targetGuildName).orElse(null);
        if (guild == null) {
            return fail("해당 길드를 찾을 수 없습니다.");
        }

        String nextName = newGuildName == null ? "" : newGuildName.trim();
        if (!GUILD_NAME_PATTERN.matcher(nextName).matches()) {
            return fail("길드명은 2~12자, 한글/영문/숫자/_ 만 사용할 수 있습니다.");
        }

        String currentNormalized = normalizeGuildName(guild.getName());
        String nextNormalized = normalizeGuildName(nextName);
        if (!currentNormalized.equals(nextNormalized) && guildIdByNormalizedName.containsKey(nextNormalized)) {
            return fail("같은 이름의 길드가 이미 존재합니다.");
        }

        String previousName = guild.getName();
        guildIdByNormalizedName.remove(currentNormalized);
        guild.setName(nextName);
        guildIdByNormalizedName.put(nextNormalized, guild.getId());

        Result refresh = refreshGuildVisualState(guild);
        if (!refresh.success()) {
            guild.setName(previousName);
            guildIdByNormalizedName.remove(nextNormalized);
            guildIdByNormalizedName.put(currentNormalized, guild.getId());
            return refresh;
        }
        save();
        return ok("길드 이름이 변경되었습니다: " + guild.getColoredName());
    }

    public Result adminSetGuildColor(String targetGuildName, String rawColor) {
        Guild guild = getGuildByName(targetGuildName).orElse(null);
        if (guild == null) {
            return fail("해당 길드를 찾을 수 없습니다.");
        }

        Optional<String> parsedColor = GuildColor.parse(rawColor);
        if (parsedColor.isEmpty()) {
            return fail("색상 입력이 올바르지 않습니다. 예시: " + GuildColor.examples());
        }
        String previousColor = guild.getColorCode();
        guild.setColorCode(parsedColor.get());

        Result refresh = refreshGuildVisualState(guild);
        if (!refresh.success()) {
            guild.setColorCode(previousColor);
            return refresh;
        }
        save();
        return ok("길드 색상이 변경되었습니다: " + guild.getColoredName());
    }

    public Result adminSetGuildPlayer(String targetGuildName, String targetName, String action) {
        Guild guild = getGuildByName(targetGuildName).orElse(null);
        if (guild == null) {
            return fail("해당 길드를 찾을 수 없습니다.");
        }
        if (!"추방".equalsIgnoreCase(action)) {
            return fail("플레이어설정은 현재 '추방'만 지원합니다.");
        }
        UUID targetId = findGuildMemberByName(guild, targetName).orElse(null);
        if (targetId == null) {
            return fail("해당 길드원은 존재하지 않습니다.");
        }
        if (guild.getLeaderId().equals(targetId)) {
            return fail("리더는 추방할 수 없습니다.");
        }

        removeMemberInternal(guild, targetId);
        save();

        notifyOnline(targetId, "§c관리자에 의해 길드에서 추방되었습니다: " + guild.getName());
        return ok("길드원 추방 완료: " + resolveName(targetId));
    }

    public Result requestJoin(Player requester, String targetGuildName) {
        if (isInGuild(requester.getUniqueId())) {
            return fail("이미 길드에 가입되어 있습니다.");
        }
        Guild guild = getGuildByName(targetGuildName).orElse(null);
        if (guild == null) {
            return fail("해당 길드를 찾을 수 없습니다.");
        }

        Set<UUID> requests = joinRequestsByGuildId.computeIfAbsent(guild.getId(), key -> new LinkedHashSet<>());
        if (!requests.add(requester.getUniqueId())) {
            return fail("이미 가입 요청을 보낸 상태입니다.");
        }
        save();

        Player leader = Bukkit.getPlayer(guild.getLeaderId());
        if (leader != null && leader.isOnline()) {
            leader.sendMessage("§e[길드] " + requester.getName() + " 님이 길드 가입을 요청했습니다.");
            leader.sendMessage("§7수락: /길드 수락 " + requester.getName() + "  |  거절: /길드 거절 " + requester.getName());
        }
        return ok("길드 가입 요청을 보냈습니다: " + guild.getName());
    }

    public Result invitePlayer(Player leader, Player target) {
        Guild guild = getGuildOf(leader.getUniqueId()).orElse(null);
        if (guild == null) {
            return fail("소속된 길드가 없습니다.");
        }
        if (!guild.getLeaderId().equals(leader.getUniqueId())) {
            return fail("길드 리더만 초대할 수 있습니다.");
        }
        if (isInGuild(target.getUniqueId())) {
            return fail("대상 플레이어는 이미 길드에 가입되어 있습니다.");
        }

        invitesByTarget.put(target.getUniqueId(), new GuildInvite(guild.getId(), leader.getUniqueId()));
        save();

        target.sendMessage("§e[길드] " + guild.getColoredName() + " 길드에서 초대가 왔습니다.");
        target.sendMessage("§7수락: /길드 수락 " + guild.getName() + "  |  거절: /길드 거절 " + guild.getName());
        return ok(target.getName() + " 님에게 길드 초대를 보냈습니다.");
    }

    public Result acceptOrDenyJoinRequest(Player leader, String targetName, boolean accept) {
        Guild guild = getGuildOf(leader.getUniqueId()).orElse(null);
        if (guild == null) {
            return fail("소속된 길드가 없습니다.");
        }
        if (!guild.getLeaderId().equals(leader.getUniqueId())) {
            return fail("길드 리더만 요청을 처리할 수 있습니다.");
        }

        Set<UUID> requests = joinRequestsByGuildId.getOrDefault(guild.getId(), Set.of());
        if (requests.isEmpty()) {
            return fail("처리할 길드 가입 요청이 없습니다.");
        }

        UUID targetId = resolvePlayerFromRequest(requests, targetName).orElse(null);
        if (targetId == null) {
            return fail("해당 플레이어의 요청을 찾을 수 없습니다.");
        }

        requests.remove(targetId);
        if (accept) {
            if (isInGuild(targetId)) {
                save();
                return fail("해당 플레이어는 이미 다른 길드에 가입되어 있습니다.");
            }
            addMemberInternal(guild, targetId);
            save();
            notifyOnline(targetId, "§a길드 가입이 승인되었습니다: " + guild.getColoredName());
            return ok("가입 요청을 수락했습니다.");
        }

        save();
        notifyOnline(targetId, "§c길드 가입 요청이 거절되었습니다: " + guild.getName());
        return ok("가입 요청을 거절했습니다.");
    }

    public Result acceptOrDenyInvite(Player target, String guildOrInviter, boolean accept) {
        GuildInvite invite = invitesByTarget.get(target.getUniqueId());
        if (invite == null) {
            return fail("받은 길드 초대가 없습니다.");
        }

        Guild guild = guildsById.get(invite.guildId());
        if (guild == null) {
            invitesByTarget.remove(target.getUniqueId());
            save();
            return fail("초대 길드가 더 이상 존재하지 않습니다.");
        }

        if (guildOrInviter != null && !guildOrInviter.isBlank()) {
            String normalized = guildOrInviter.trim().toLowerCase(Locale.ROOT);
            String guildName = guild.getName().toLowerCase(Locale.ROOT);
            String inviterName = resolveName(invite.inviterId()).toLowerCase(Locale.ROOT);
            if (!guildName.equals(normalized) && !inviterName.equals(normalized)) {
                return fail("해당 길드 초대를 찾을 수 없습니다.");
            }
        }

        invitesByTarget.remove(target.getUniqueId());
        if (!accept) {
            save();
            notifyOnline(invite.inviterId(), "§e" + target.getName() + " 님이 길드 초대를 거절했습니다.");
            return ok("길드 초대를 거절했습니다.");
        }

        if (isInGuild(target.getUniqueId())) {
            save();
            return fail("이미 다른 길드에 가입되어 있습니다.");
        }
        addMemberInternal(guild, target.getUniqueId());
        save();

        notifyOnline(invite.inviterId(), "§a" + target.getName() + " 님이 길드 초대를 수락했습니다.");
        return ok("길드에 가입되었습니다: " + guild.getColoredName());
    }

    public List<String> getGuildListLines() {
        if (guildsById.isEmpty()) {
            return List.of("§7생성된 길드가 없습니다.");
        }
        List<Guild> sorted = guildsById.values().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add("§e길드 목록");
        for (Guild guild : sorted) {
            lines.add("§f- " + guild.getColoredName() + " §7(" + guild.getMembers().size() + "명)");
        }
        return lines;
    }

    public List<String> getGuildInfoLines(Guild guild) {
        List<String> lines = new ArrayList<>();
        lines.add("§e길드 정보");
        lines.add("§7이름: §f" + guild.getColoredName());
        lines.add("§7리더: §f" + resolveName(guild.getLeaderId()));
        lines.add("§7인원: §f" + guild.getMembers().size() + "명");
        lines.add("§7구성원:");
        for (UUID memberId : guild.getMembers()) {
            String leaderMark = memberId.equals(guild.getLeaderId()) ? " §6(리더)" : "";
            lines.add("§f- " + resolveName(memberId) + leaderMark);
        }
        return lines;
    }

    public Optional<Guild> getGuildByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String guildId = guildIdByNormalizedName.get(normalizeGuildName(name.trim()));
        if (guildId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(guildsById.get(guildId));
    }

    public Optional<Guild> getGuildOf(UUID playerId) {
        String guildId = memberGuildId.get(playerId);
        if (guildId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(guildsById.get(guildId));
    }

    public boolean isInGuild(UUID playerId) {
        return memberGuildId.containsKey(playerId);
    }

    public boolean isGuildLeader(UUID playerId) {
        return getGuildOf(playerId).map(guild -> guild.getLeaderId().equals(playerId)).orElse(false);
    }

    public List<String> getPendingJoinRequestNamesForLeader(UUID leaderId) {
        List<String> pending = new ArrayList<>();
        for (Guild guild : guildsById.values()) {
            if (!guild.getLeaderId().equals(leaderId)) {
                continue;
            }
            Set<UUID> requests = joinRequestsByGuildId.getOrDefault(guild.getId(), Set.of());
            for (UUID requesterId : requests) {
                pending.add(resolveName(requesterId) + " (길드: " + guild.getName() + ")");
            }
        }
        return pending;
    }

    public boolean hasPendingJoinRequests(UUID leaderId) {
        for (Guild guild : guildsById.values()) {
            if (!guild.getLeaderId().equals(leaderId)) {
                continue;
            }
            Set<UUID> requests = joinRequestsByGuildId.getOrDefault(guild.getId(), Set.of());
            if (!requests.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Optional<String> getInviteMessage(UUID playerId) {
        GuildInvite invite = invitesByTarget.get(playerId);
        if (invite == null) {
            return Optional.empty();
        }
        Guild guild = guildsById.get(invite.guildId());
        if (guild == null) {
            return Optional.empty();
        }
        return Optional.of("§e[길드] " + guild.getColoredName() + " 길드 초대가 대기중입니다. /길드 수락 " + guild.getName());
    }

    public Collection<Guild> getGuilds() {
        return guildsById.values();
    }

    public List<String> getGuildMemberNames(String guildName) {
        Guild guild = getGuildByName(guildName).orElse(null);
        if (guild == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (UUID memberId : guild.getMembers()) {
            names.add(resolveName(memberId));
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public void save() {
        config.set("guilds", null);
        for (Guild guild : guildsById.values()) {
            String base = "guilds." + guild.getId();
            config.set(base + ".name", guild.getName());
            config.set(base + ".color", guild.getColorCode());
            config.set(base + ".leader", guild.getLeaderId().toString());
            config.set(base + ".lp_group", guild.getLuckPermsGroupName());
            List<String> members = guild.getMembers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            config.set(base + ".members", members);
        }

        config.set("requests.join", null);
        for (Map.Entry<String, Set<UUID>> entry : joinRequestsByGuildId.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String key = "requests.join." + entry.getKey();
            List<String> values = entry.getValue().stream().map(UUID::toString).toList();
            config.set(key, values);
        }

        config.set("requests.invites", null);
        for (Map.Entry<UUID, GuildInvite> entry : invitesByTarget.entrySet()) {
            String base = "requests.invites." + entry.getKey();
            GuildInvite invite = entry.getValue();
            config.set(base + ".guild_id", invite.guildId());
            config.set(base + ".inviter", invite.inviterId().toString());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("guilds.yml 저장 실패: " + e.getMessage());
        }

        profileManager.save();
    }

    private void load() {
        guildsById.clear();
        guildIdByNormalizedName.clear();
        memberGuildId.clear();
        joinRequestsByGuildId.clear();
        invitesByTarget.clear();

        this.config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection guildSection = config.getConfigurationSection("guilds");
        if (guildSection != null) {
            for (String guildId : guildSection.getKeys(false)) {
                String base = "guilds." + guildId;
                try {
                    String name = config.getString(base + ".name", "");
                    String color = config.getString(base + ".color", "§f");
                    String leaderRaw = config.getString(base + ".leader", "");
                    String lpGroup = config.getString(base + ".lp_group", "dw_guild_" + guildId.substring(0, 8));
                    if (name.isBlank() || leaderRaw.isBlank()) {
                        continue;
                    }
                    UUID leaderId = UUID.fromString(leaderRaw);
                    Guild guild = new Guild(guildId, name, color, leaderId, lpGroup);
                    List<String> members = config.getStringList(base + ".members");
                    if (members.isEmpty()) {
                        guild.addMember(leaderId);
                    } else {
                        for (String rawMember : members) {
                            try {
                                guild.addMember(UUID.fromString(rawMember));
                            } catch (IllegalArgumentException ignored) {
                                // skip malformed uuid
                            }
                        }
                    }
                    guildsById.put(guildId, guild);
                    guildIdByNormalizedName.put(normalizeGuildName(name), guildId);
                    for (UUID member : guild.getMembers()) {
                        memberGuildId.put(member, guildId);
                        profileManager.get(member).setGuildName(guild.getColoredName());
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip malformed guild section
                }
            }
        }

        ConfigurationSection joinSection = config.getConfigurationSection("requests.join");
        if (joinSection != null) {
            for (String guildId : joinSection.getKeys(false)) {
                Set<UUID> requests = new LinkedHashSet<>();
                for (String raw : config.getStringList("requests.join." + guildId)) {
                    try {
                        requests.add(UUID.fromString(raw));
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed uuid
                    }
                }
                if (!requests.isEmpty()) {
                    joinRequestsByGuildId.put(guildId, requests);
                }
            }
        }

        ConfigurationSection inviteSection = config.getConfigurationSection("requests.invites");
        if (inviteSection != null) {
            for (String targetRaw : inviteSection.getKeys(false)) {
                try {
                    UUID targetId = UUID.fromString(targetRaw);
                    String base = "requests.invites." + targetRaw;
                    String guildId = config.getString(base + ".guild_id", "");
                    String inviterRaw = config.getString(base + ".inviter", "");
                    if (guildId.isBlank() || inviterRaw.isBlank()) {
                        continue;
                    }
                    if (!guildsById.containsKey(guildId)) {
                        continue;
                    }
                    UUID inviterId = UUID.fromString(inviterRaw);
                    invitesByTarget.put(targetId, new GuildInvite(guildId, inviterId));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed invite entry
                }
            }
        }
    }

    private String generateLuckPermsGroupName(String guildName) {
        String baseSlug = guildName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (baseSlug.isBlank()) {
            baseSlug = "guild";
        }
        String base = "dw_guild_" + baseSlug;
        String candidate = base;
        int suffix = 1;
        Set<String> used = guildsById.values().stream()
                .map(guild -> guild.getLuckPermsGroupName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
        while (used.contains(candidate.toLowerCase(Locale.ROOT))
                || luckPerms.getGroupManager().getGroup(candidate) != null) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    private void addMemberInternal(Guild guild, UUID playerId) {
        guild.addMember(playerId);
        memberGuildId.put(playerId, guild.getId());
        profileManager.get(playerId).setGuildName(guild.getColoredName());
        addLuckPermsParent(playerId, guild.getLuckPermsGroupName());
    }

    private void removeMemberInternal(Guild guild, UUID playerId) {
        guild.removeMember(playerId);
        memberGuildId.remove(playerId);
        profileManager.get(playerId).setGuildName(DEFAULT_GUILD_NAME);
        removeLuckPermsParent(playerId, guild.getLuckPermsGroupName());
    }

    private void addLuckPermsParent(UUID playerId, String groupName) {
        if (luckPerms == null) {
            return;
        }
        try {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            luckPerms.getUserManager().modifyUser(playerId, user -> user.data().add(node)).join();
        } catch (CompletionException ex) {
            plugin.getLogger().warning("길드 그룹 부여 실패(" + playerId + "): " + ex.getMessage());
        }
    }

    private void removeLuckPermsParent(UUID playerId, String groupName) {
        if (luckPerms == null) {
            return;
        }
        try {
            InheritanceNode node = InheritanceNode.builder(groupName).build();
            luckPerms.getUserManager().modifyUser(playerId, user -> user.data().remove(node)).join();
        } catch (CompletionException ex) {
            plugin.getLogger().warning("길드 그룹 제거 실패(" + playerId + "): " + ex.getMessage());
        }
    }

    private Optional<UUID> findGuildMemberByName(Guild guild, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String target = rawName.trim().toLowerCase(Locale.ROOT);
        for (UUID memberId : guild.getMembers()) {
            String name = resolveName(memberId);
            if (name.equalsIgnoreCase(target)) {
                return Optional.of(memberId);
            }
        }
        return Optional.empty();
    }

    private Result refreshGuildVisualState(Guild guild) {
        if (luckPerms == null) {
            return fail("LuckPerms를 사용할 수 없어 길드 태그를 갱신하지 못했습니다.");
        }

        Group group = luckPerms.getGroupManager().getGroup(guild.getLuckPermsGroupName());
        if (group == null) {
            return fail("길드 그룹을 찾지 못했습니다: " + guild.getLuckPermsGroupName());
        }

        try {
            group.data().clear(node -> node instanceof SuffixNode);
            group.data().add(SuffixNode.builder(guild.getSuffixValue(), 50).build());
            luckPerms.getGroupManager().saveGroup(group).join();
            for (UUID memberId : guild.getMembers()) {
                profileManager.get(memberId).setGuildName(guild.getColoredName());
            }
            return ok("길드 상태가 갱신되었습니다.");
        } catch (CompletionException ex) {
            plugin.getLogger().warning("길드 태그 갱신 실패(" + guild.getName() + "): " + ex.getMessage());
            return fail("길드 태그를 갱신하지 못했습니다.");
        }
    }

    private Optional<UUID> resolvePlayerFromRequest(Set<UUID> requests, String rawName) {
        if (requests.isEmpty()) {
            return Optional.empty();
        }
        if (rawName == null || rawName.isBlank()) {
            if (requests.size() == 1) {
                return Optional.of(requests.iterator().next());
            }
            return Optional.empty();
        }
        String target = rawName.trim().toLowerCase(Locale.ROOT);
        for (UUID requesterId : requests) {
            if (resolveName(requesterId).equalsIgnoreCase(target)) {
                return Optional.of(requesterId);
            }
        }
        return Optional.empty();
    }

    private String resolveName(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        return offline.getName() == null ? playerId.toString() : offline.getName();
    }

    private void notifyOnline(UUID playerId, String message) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            online.sendMessage(message);
        }
    }

    private LuckPerms findLuckPerms() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            plugin.getLogger().warning("LuckPerms가 감지되지 않았습니다. 길드 기능이 비활성화됩니다.");
            return null;
        }
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("LuckPerms API를 가져오지 못했습니다: " + ex.getMessage());
            return null;
        }
    }

    private static String normalizeGuildName(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }

    private static Result ok(String message) {
        return new Result(true, message);
    }

    private static Result fail(String message) {
        return new Result(false, message);
    }

    public record Result(boolean success, String message) {
        public void sendTo(CommandSender sender) {
            sender.sendMessage((success ? "§a" : "§c") + message);
        }
    }
}
