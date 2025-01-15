import type { Artwork } from '$lib/types/artwork';
import type { ArtworkFilters } from '$lib/components/homepage/artwork/ArtworkFilters.svelte';
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
			filters.objectType.length > 0 ||
			filters.materials.length > 0 ||
			filters.geographicLocation.length > 0 ||
			filters.era.length > 0 ||
			filters.onDisplay ||
			filters.hasImage
		);
	}

	static filterArtworks(artworks: Artwork[], filters: ArtworkFilters): Artwork[] {
		if (!this.hasActiveFilters(filters)) {
			return artworks;
		}

		return artworks.filter((artwork) => {
			// Search term filter
			if (filters.searchTerm.length > 0) {
				const matchesTerm = filters.searchTerm.some((term: string) => {
					const searchTerm = term.toLowerCase();
					switch (filters.searchField) {
						case 'title':
							return artwork.title.toLowerCase().includes(searchTerm);
						case 'artist':
							return artwork.artist?.toLowerCase().includes(searchTerm) ?? false;
						case 'all':
							return (
								artwork.title.toLowerCase().includes(searchTerm) ||
								(artwork.artist?.toLowerCase().includes(searchTerm) ?? false) ||
								(artwork.medium?.toLowerCase().includes(searchTerm) ?? false)
							);
						default:
							return false;
					}
				});
				if (!matchesTerm) return false;
			}

			// Era filter
			if (filters.era.length > 0 && artwork.creationDate) {
				try {
					const year = DateUtils.extractYear(artwork.creationDate);
					const matchesEra = filters.era.some((period: string) => {
						return DateUtils.isYearInPeriod(year, period);
					});
					if (!matchesEra) return false;
					// eslint-disable-next-line @typescript-eslint/no-unused-vars
				} catch (error) {
					return false;
				}
			}

			// Other filters with null checks
			if (filters.onDisplay && !artwork.isOnDisplay) return false;
			if (filters.hasImage && !artwork.imageUrl) return false;
			if (filters.objectType.length > 0) {
				const classification = filters.objectType[0];
				if (!artwork.classification || artwork.classification !== classification) {
					return false;
				}
			}

			// Materials filter (only applies when materials are selected)
			if (filters.materials.length > 0) {
				if (!artwork.medium || !filters.materials.some((material : string) =>
					artwork.medium.includes(material))) {
					return false;
				}
			}

			return !(filters.geographicLocation.length > 0 &&
				(!artwork.geographicLocation ||
					!filters.geographicLocation.includes(artwork.geographicLocation)));
		});
	}

	static getDefaultFilters(): ArtworkFilters {
		return {
			searchTerm: [],
			searchField: 'all',
			objectType: [],
			materials: [],
			geographicLocation: [],
			culture: [],
			era: [],
			onDisplay: false,
			hasImage: true
		};
	}
}