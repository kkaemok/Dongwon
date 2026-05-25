package org.kkaemok.dongwon.job;

import org.bukkit.potion.PotionEffectType;
import org.kkaemok.dongwon.job.jobs.AntiDebufferJob;
import org.kkaemok.dongwon.job.jobs.BufferJob;
import org.kkaemok.dongwon.job.jobs.CheonhoJob;
import org.kkaemok.dongwon.job.jobs.FarmerJob;
import org.kkaemok.dongwon.job.jobs.FishermanJob;
import org.kkaemok.dongwon.job.jobs.GumihoJob;
import org.kkaemok.dongwon.job.jobs.HealerJob;
import org.kkaemok.dongwon.job.jobs.JobDefinition;
import org.kkaemok.dongwon.job.jobs.MinerJob;
import org.kkaemok.dongwon.job.jobs.NoneJob;
import org.kkaemok.dongwon.job.jobs.OrcJob;
import org.kkaemok.dongwon.job.jobs.ShinnongJob;
import org.kkaemok.dongwon.job.jobs.SunPriestJob;
import org.kkaemok.dongwon.job.jobs.SwordsmanJob;
import org.kkaemok.dongwon.job.jobs.WarriorJob;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JobRegistry {
    private final Map<JobType, JobDefinition> definitions = new EnumMap<>(JobType.class);
    private final Set<PotionEffectType> managedPassiveEffects = new HashSet<>();
    private final JobDefinition fallback = new NoneJob();

    public JobRegistry() {
        register(fallback);
        register(new MinerJob());
        register(new WarriorJob());
        register(new SwordsmanJob());
        register(new FishermanJob());
        register(new FarmerJob());
        register(new BufferJob());
        register(new HealerJob());
        register(new GumihoJob());
        register(new OrcJob());
        register(new SunPriestJob());
        register(new AntiDebufferJob());
        register(new CheonhoJob());
        register(new ShinnongJob());
    }

    public JobDefinition get(JobType type) {
        return definitions.getOrDefault(type, fallback);
    }

    public Set<PotionEffectType> getManagedPassiveEffects() {
        return Set.copyOf(managedPassiveEffects);
    }

    private void register(JobDefinition definition) {
        definitions.put(definition.getType(), definition);
        managedPassiveEffects.addAll(definition.getManagedPassiveEffects());
    }
}
