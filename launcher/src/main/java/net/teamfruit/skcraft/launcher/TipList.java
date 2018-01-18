/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package net.teamfruit.skcraft.launcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import com.skcraft.concurrency.DefaultProgress;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.LauncherException;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.HttpRequest;
import com.skcraft.launcher.util.SharedLocale;

import lombok.NonNull;
import lombok.extern.java.Log;
import net.teamfruit.skcraft.launcher.model.tips.TipInfo;
import net.teamfruit.skcraft.launcher.model.tips.TipInfoList;

/**
 * Stores the list of tips.
 */
@Log
public class TipList {

	private final Launcher launcher;
	private final List<Tip> tips = new ArrayList<Tip>();

	/**
	 * Create a new tip list.
	 *
	 * @param launcher the launcher
	 */
	public TipList(@NonNull final Launcher launcher) {
		this.launcher = launcher;
	}

	/**
	 * Get the tip
	 *
	 * @return the tip list
	 */
	public List<Tip> getTipList() {
		return Collections.unmodifiableList(this.tips);
	}

	/**
	 * Create a worker that loads the list of tips from disk and from
	 * the remote list of packages.
	 *
	 * @return the worker
	 */
	public Enumerator createEnumerator() {
		return new Enumerator();
	}

	public final class Enumerator implements Callable<TipList>, ProgressObservable {
		private ProgressObservable progress = new DefaultProgress(-1, null);

		private Enumerator() {
		}

		@Override
		public TipList call() throws Exception {
			log.info("Enumerating tips list...");
			final List<Tip> remote = new ArrayList<Tip>();

			this.progress = new DefaultProgress(0.3, SharedLocale.tr("tipLoader.checkingRemote"));

			try {
				final URL tipsURL = TipList.this.launcher.getTipsURL();

				final TipInfoList tipInfos = HttpRequest
						.get(tipsURL)
						.execute()
						.expectResponseCode(200)
						.returnContent()
						.asJson(TipInfoList.class);

				if (tipInfos.getMinimumVersion()>TipInfoList.MIN_VERSION)
					throw new LauncherException("Update required", SharedLocale.tr("errors.updateRequiredError"));

                for (final TipInfo tipInfo : tipInfos.getTips()) {
					final Tip tip = new Tip();
					tip.setDesc(tipInfo.getDesc());
					tip.setThumb(tipInfo.getThumb());
					tip.setThumbImage(SwingHelper.createImage(tipInfo.getThumb()));
					if (tip.getDesc()!=null&&tip.getThumb()!=null&&tip.getThumbImage()!=null)
						remote.add(tip);
                }

			} catch (final IOException e) {
				log.log(Level.WARNING, "The list of tips could not be downloaded.", e);
			} finally {
				synchronized (TipList.this) {
					TipList.this.tips.clear();
					TipList.this.tips.addAll(remote);

					log.info(TipList.this.tips.size()+" tip(s) enumerated.");
				}
			}

			return TipList.this;
		}

		@Override
		public double getProgress() {
			return -1;
		}

		@Override
		public String getStatus() {
			return this.progress.getStatus();
		}
	}
}