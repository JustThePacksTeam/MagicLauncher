package net.teamfruit.skcraft.launcher.util;

import lombok.NonNull;

public enum ImageSizes {
		RAW {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				return new SizeData(w, h);
			}
		},
		MAX {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				return new SizeData(maxw, maxh);
			}
		},
		WIDTH {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				return new SizeData(maxw, h*maxw/w);
			}
		},
		HEIGHT {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				return new SizeData(w*maxh/h, maxh);
			}
		},
		INNER {
			@Override
			public @NonNull SizeData size(final float w, final float h, float maxw, float maxh) {
				if (w<0)
					maxw *= -1;
				if (h<0)
					maxh *= -1;
				final boolean b = w/maxw>h/maxh;
				return new SizeData(b ? maxw : w*maxh/h, b ? h*maxw/w : maxh);
			}
		},
		OUTER {
			@Override
			public @NonNull SizeData size(final float w, final float h, float maxw, float maxh) {
				if (w<0)
					maxw *= -1;
				if (h<0)
					maxh *= -1;
				final boolean b = w/maxw<h/maxh;
				return new SizeData(b ? maxw : w*maxh/h, b ? h*maxw/w : maxh);
			}
		},
		WIDTH_LIMIT {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				if (w<maxw)
					return new SizeData(w, h);
				else
					return new SizeData(maxw, maxw*h/w);
			}
		},
		HEIGHT_LIMIT {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				if (h<maxh)
					return new SizeData(w, h);
				else
					return new SizeData(maxh*w/h, maxh);
			}
		},
		LIMIT {
			@Override
			public @NonNull SizeData size(final float w, final float h, final float maxw, final float maxh) {
				if (w>h)
					if (w<maxw)
						return new SizeData(w, h);
					else
						return new SizeData(maxw, maxw*h/w);
				else if (h<maxh)
					return new SizeData(w, h);
				else
					return new SizeData(maxh*w/h, maxh);
			}
		},
		;

		public abstract @NonNull SizeData size(float w, float h, float maxw, float maxh);
}