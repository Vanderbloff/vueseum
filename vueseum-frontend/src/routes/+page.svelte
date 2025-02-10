<script lang="ts">
	import { Tabs, TabsContent, TabsList, TabsTrigger } from '$lib/components/ui/tabs';
	import { Card, CardContent } from "$lib/components/ui/card";
	import type { PageData } from './$types';

	let { data } = $props<{ data: PageData }>();

	const state = $state({
		isLoading: false,
		error: null as string | null
	});

	function handleTabChange(value: string) {
		console.log('Tab changed:', value);
	}
</script>

<main class="container mx-auto p-4">
	<Tabs value={data.initialTab} onValueChange={handleTabChange}>
		<div class="flex justify-center mb-4">
			<TabsList class="grid w-[400px] grid-cols-2">
				<TabsTrigger value="artworks">Search Artworks</TabsTrigger>
				<TabsTrigger value="tours">Tours</TabsTrigger>
			</TabsList>
		</div>

		<TabsContent value="artworks">
			<Card>
				<CardContent>
					<h2>Artworks Content</h2>
					<p>Debug mode - basic tabs test</p>
				</CardContent>
			</Card>
		</TabsContent>

		<TabsContent value="tours">
			<Card>
				<CardContent>
					<h2>Tours Content</h2>
					<p>Debug mode - basic tabs test</p>
				</CardContent>
			</Card>
		</TabsContent>
	</Tabs>
</main>