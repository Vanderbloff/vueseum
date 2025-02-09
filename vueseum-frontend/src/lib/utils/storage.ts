export class StorageManager {
	private static PREFIX = 'vueseum_';

	static set<T>(key: string, value: T): void {
		try {
			localStorage.setItem(
				this.PREFIX + key,
				JSON.stringify(value)
			);
		} catch (error) {
			console.warn('Failed to save to localStorage:', error);
		}
	}

	static get<T>(key: string, defaultValue: T): T {
		try {
			const item = localStorage.getItem(this.PREFIX + key);
			return item ? JSON.parse(item) : defaultValue;
		} catch (error) {
			console.warn('Failed to read from localStorage:', error);
			return defaultValue;
		}
	}

	static remove(key: string): void {
		try {
			localStorage.removeItem(this.PREFIX + key);
		} catch (error) {
			console.warn('Failed to remove from localStorage:', error);
		}
	}
}