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
			filters.country.length > 0 ||
			filters.region.length > 0 ||
			filters.culture.length > 0 ||
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

			// Object Type filter
			if (filters.objectType.length > 0) {
				const classificationMatches = artwork.classification &&
					filters.objectType.includes(artwork.classification);
				if (!classificationMatches) return false;
			}

			// Materials filter
			if (filters.materials.length > 0) {
				const materialMatches = artwork.medium &&
					filters.materials.some(material => artwork.medium?.includes(material));
				if (!materialMatches) return false;
			}

			// Geographic Location filter
			if (filters.country.length > 0) {
				const countryMatches = artwork.country === filters.country[0];
				if (!countryMatches) return false;

				if (filters.region.length > 0) {
					const regionMatches = artwork.region === filters.region[0];
					if (!regionMatches) return false;
				}
			}

			// Culture filter
			if (filters.culture.length > 0) {
				const cultureMatches = artwork.culture &&
					filters.culture.includes(artwork.culture);
				if (!cultureMatches) return false;
			}

			if (filters.era.length > 0 && artwork.creationDate) {
				try {
					const year = DateUtils.extractYear(artwork.creationDate);
					const eraMatches = filters.era.some(period =>
						DateUtils.isYearInPeriod(year, period));
					if (!eraMatches) return false;
					// eslint-disable-next-line @typescript-eslint/no-unused-vars
				} catch (error) {
					return false;
				}
			}

			if (filters.hasImage && !artwork.imageUrl) return false;

			return true;
		});
	}

	static getDefaultFilters(): ArtworkFilters {
		return {
			searchTerm: [],
			searchField: 'all',
			objectType: [],
			materials: [],
			country: [],
			region: [],
			culture: [],
			era: [],
			hasImage: true,
			museumId: []
		};
	}
}