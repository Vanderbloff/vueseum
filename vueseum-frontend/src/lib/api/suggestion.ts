// src/lib/api/suggestion.ts
import { BaseApiClient } from './base';
import type { TourPreferences } from '$lib/types/tour-preferences';
import { API_BASE_URL } from '$lib/config';

export type SuggestionType = 'ARTWORK' | 'ARTIST' | 'MEDIUM' | 'CULTURE' | 'PERIOD';

export interface Suggestion {
	value: string;
	display: string;
	count: number;
	type: SuggestionType;
}

interface ArtworkData {
	id: number;
	title: string;
	artist: string;
	medium: string;
	culture: string;
	creationDate: string;
	museum: {
		id: number;
		name: string;
	}

}

class SuggestionApiClient extends BaseApiClient {
	constructor() {
		super('/suggestions');
	}

	async getSuggestions(
		prefix: string,
		type: SuggestionType,
		museumId: string,
		preferences?: Partial<TourPreferences>
	): Promise<Suggestion[]> {
		const params = new URLSearchParams({
			prefix,
			type: type.toString(),
			museumId
		});

		if (import.meta.env.DEV) {
			return this.getDevSuggestions(prefix, type, museumId);
		}

		if (preferences?.preferredArtworks?.length) {
			preferences.preferredArtworks.forEach(artwork => {
				params.append('preferredArtworks', artwork);
			});
		}

		if (preferences?.preferredArtists?.length) {
			preferences.preferredArtists.forEach(artist => {
				params.append('preferredArtists', artist);
			});
		}

		if (preferences?.preferredMediums?.length) {
			preferences.preferredMediums.forEach(medium => {
				params.append('preferredMediums', medium);
			});
		}

		if (preferences?.preferredCultures?.length) {
			preferences.preferredCultures.forEach(culture => {
				params.append('preferredCultures', culture);
			});
		}

		if (preferences?.preferredPeriods?.length) {
			preferences.preferredPeriods.forEach(period => {
				params.append('preferredPeriods', period);
			});
		}

		try {
			return this.fetchWithError(`?${params}`);
		} catch (error) {
			console.error('Error fetching suggestions:', error);
			console.debug('Request parameters:', { prefix, type, museumId, preferences });
			throw error;
		}
	}

	private async getDevSuggestions(
		prefix: string,
		type: SuggestionType,
		museumId: string
	): Promise<Suggestion[]> {
		// Simulate network delay
		await new Promise(resolve => setTimeout(resolve, 300));

		// Get artworks data
		const response = await fetch(`${API_BASE_URL}/artworks`);
		if (!response.ok) {
			throw new Error(`Failed to fetch artworks: ${response.statusText}`);
		}
		const artworks: ArtworkData[] = await response.json();

		// Filter by museum if museumId is provided
		const museumArtworks = museumId
			? artworks.filter(a => a.museum?.id.toString() === museumId)
			: artworks;

		const lowercasePrefix = prefix.toLowerCase();

		switch (type) {
			case 'ARTWORK':
				return museumArtworks
					.filter(a => a.title.toLowerCase().includes(lowercasePrefix))
					.map(a => ({
						value: a.title,
						display: `${a.title} (${a.creationDate})`,
						count: 1,
						type: 'ARTWORK'
					}));

			case 'ARTIST':
				{ const uniqueArtists = new Map();
				museumArtworks.forEach(a => {
					if (a.artist.toLowerCase().includes(lowercasePrefix)) {
						uniqueArtists.set(a.artist, (uniqueArtists.get(a.artist) || 0) + 1);
					}
				});
				return Array.from(uniqueArtists).map(([artist, count]) => ({
					value: artist,
					display: artist,
					count: count as number,
					type: 'ARTIST'
				})); }

			case 'MEDIUM':
				{ const uniqueMediums = new Map();
				museumArtworks.forEach(a => {
					if (a.medium.toLowerCase().includes(lowercasePrefix)) {
						uniqueMediums.set(a.medium, (uniqueMediums.get(a.medium) || 0) + 1);
					}
				});
				return Array.from(uniqueMediums).map(([medium, count]) => ({
					value: medium,
					display: medium,
					count: count as number,
					type: 'MEDIUM'
				})); }

			case 'CULTURE':
				{ const uniqueCultures = new Map();
				museumArtworks.forEach(a => {
					if (a.culture.toLowerCase().includes(lowercasePrefix)) {
						uniqueCultures.set(a.culture, (uniqueCultures.get(a.culture) || 0) + 1);
					}
				});
				return Array.from(uniqueCultures).map(([culture, count]) => ({
					value: culture,
					display: culture,
					count: count as number,
					type: 'CULTURE'
				})); }

			default:
				return [];
		}
	}
}

export const suggestionApi = new SuggestionApiClient();