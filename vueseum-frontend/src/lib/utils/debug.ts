export const debugLog = (message: string, ...args: any[]) => {
	// Allow logging in both dev and prod for now
	if (typeof window !== 'undefined') {
		console.log(message, ...args);
	}
};

export const debugError = (message: string, ...args: any[]) => {
	if (typeof window !== 'undefined') {
		console.error(message, ...args);
	}
};