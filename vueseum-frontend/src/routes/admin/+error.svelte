<script lang="ts">
	import { page } from '$app/state';
	import { Button } from "$lib/components/ui/button";
	import { Card, CardContent, CardHeader, CardTitle } from "$lib/components/ui/card";
	import { ArrowLeft } from 'lucide-svelte';

	const errorMessage = $derived(() => {
		if (page.error?.message) {
			return page.error.message;
		}

		if (page.status === 404) {
			return 'Admin page not found';
		}

		return 'An error occurred while accessing the admin section';
	});
</script>

<div class="container mx-auto px-4 py-12">
	<div class="mb-6">
		<Button
			variant="ghost"
			class="gap-2 text-foreground"
			onclick={() => window.history.back()}
		>
			<ArrowLeft class="h-5 w-5" />
			Back
		</Button>
	</div>

	<Card>
		<CardHeader>
			<CardTitle>Admin Error</CardTitle>
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