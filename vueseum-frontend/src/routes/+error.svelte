<script lang="ts">
	import { page } from '$app/state';
	import { Button } from "$lib/components/ui/button";
	import { Card, CardContent, CardHeader, CardTitle } from "$lib/components/ui/card";

	const errorMessage = $derived(() => {
		if (page.error?.message) {
			return page.error.message;
		}

		if (page.status === 404) {
			return 'Page not found';
		}

		return 'Something went wrong';
	});
</script>

<div class="container mx-auto px-4 py-12">
	<Card>
		<CardHeader>
			<CardTitle>Error</CardTitle>
		</CardHeader>
		<CardContent>
			<div class="text-center">
				<p class="mb-6 text-muted-foreground">
					{errorMessage}
				</p>
				<div class="space-x-4">
					<Button
						variant="outline"
						onclick={() => window.location.reload()}
					>
						Try Again
					</Button>
					<Button
						onclick={() => window.location.href = '/'}
					>
						Return Home
					</Button>
				</div>
			</div>
		</CardContent>
	</Card>
</div>