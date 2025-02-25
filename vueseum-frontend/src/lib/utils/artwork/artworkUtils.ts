import type { Artwork } from '$lib/types/artwork';
import type { ArtworkFilters } from '$lib/types/filters';
import { DateUtils } from '../dateUtils';

export class ArtworkUtils {
	static sortArtworks(artworks: Artwork[], field: string, direction: 'asc' | 'desc'): Artwork[] {
		return [...artworks].sort((a, b) => {
			let comparison = 0;

			switch (field) {
				case 'title':
					comparison = a.title.localeCompare(b.title);
					break;
				case 'artist':
					comparison = (a.artist || '').localeCompare(b.artist || '');
					break;
				case 'date': {
					const dateA = a.creationDate ? DateUtils.extractYear(a.creationDate) : 0;
					const dateB = b.creationDate ? DateUtils.extractYear(b.creationDate) : 0;
					comparison = dateA - dateB;
					break;
				}
				default:
					return 0;
			}

			return direction === 'asc' ? comparison : -comparison;
		});
	}

	static hasActiveFilters(filters: ArtworkFilters): boolean {
		return (
			filters.searchTerm.length > 0 ||
			filters.category.length > 0 ||
			filters.origin.length > 0 ||
			filters.era.length > 0 ||
			filters.museumId.length > 0 ||
			filters.hasImage
		);
	}

	static filterArtworks(artworks: Artwork[], filters: ArtworkFilters): Artwork[] {
		if (!this.hasActiveFilters(filters)) {
			return artworks;
		}

		return artworks.filter((artwork) => {
			// Has Image filter
			if (filters.hasImage) {
				const hasValidImage = Boolean(artwork.primaryImageUrl || artwork.thumbnailImageUrl);
				if (!hasValidImage) return false;
			}

			// Search term filter
			if (filters.searchTerm.length > 0) {
				const matchesTerm = filters.searchTerm.some((term: string) => {
					const searchTerm = term.toLowerCase();
					switch (filters.searchField) {
						case 'title':
							return artwork.title.toLowerCase().includes(searchTerm);
						case 'artist':
							return artwork.artist?.toLowerCase().includes(searchTerm) ?? false;
						case 'culture':
							return artwork.culture?.toLowerCase().includes(searchTerm) ?? false;
						case 'all':
							return (
								artwork.title.toLowerCase().includes(searchTerm) ||
								(artwork.artist?.toLowerCase().includes(searchTerm) ?? false) ||
								(artwork.culture?.toLowerCase().includes(searchTerm) ?? false)
							);
						default:
							return false;
					}
				});
				if (!matchesTerm) return false;
			}

			// Category filter (combines objectType and medium)
			if (filters.category.length > 0) {
				const categoryValue = filters.category[0].toLowerCase();
				const matchesCategory =
					(artwork.classification?.toLowerCase().includes(categoryValue) ?? false) ||
					(artwork.medium?.toLowerCase().includes(categoryValue) ?? false);
				if (!matchesCategory) return false;
			}

			// Origin filter (combines country, region, and culture)
			if (filters.origin.length > 0) {
				const originValue = filters.origin[0].toLowerCase();
				const matchesOrigin =
					(artwork.country?.toLowerCase().includes(originValue) ?? false) ||
					(artwork.region?.toLowerCase().includes(originValue) ?? false) ||
					(artwork.culture?.toLowerCase().includes(originValue) ?? false);
				if (!matchesOrigin) return false;
			}

			// Era filter
			if (filters.era.length > 0 && artwork.creationDate) {
				try {
					const year = DateUtils.extractYear(artwork.creationDate);
					const eraMatches = filters.era.some(period =>
						DateUtils.isYearInPeriod(year, period));
					if (!eraMatches) return false;
				} catch {
					return false;
				}
			}

			return true;
		});
	}

	static getDefaultFilters(): ArtworkFilters {
		return {
			searchTerm: [],
			searchField: 'all',
			category: [],
			origin: [],
			era: [],
			hasImage: false,
			museumId: []
		};
	}
}