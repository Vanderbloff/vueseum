# ArtworkCard Component Test Checklist

## Content Display

### Title Display
- [ ] Renders title correctly
- [ ] Long titles are properly truncated (line-clamp-2)
- [ ] Title maintains proper spacing with other elements
- [ ] Font size and weight are correct
- [ ] Special characters in titles render correctly

### Artist Information
- [ ] Artist name displays correctly
- [ ] Attribution indicator (?) appears for uncertain attributions
- [ ] Attribution indicator has correct styling and placement
- [ ] Attribution indicator and artist name maintain proper spacing
- [ ] Long artist names handle appropriately

### Date and Additional Information
- [ ] Creation date renders correctly
- [ ] Department and medium show when available
- [ ] Bullet separator (•) appears correctly between department and medium
- [ ] Information maintains proper alignment and spacing
- [ ] Text colors match design specification

## Image Handling

### Image Loading
- [ ] Images load correctly in aspect ratio container (4:3)
- [ ] Loading state shows appropriate background
- [ ] Images maintain proper object-fit (cover)
- [ ] Alternative text is present and descriptive
- [ ] Image scaling on hover works smoothly

### Image Edge Cases
- [ ] Handles missing images gracefully
- [ ] Maintains layout when image fails to load
- [ ] Very tall/wide images display correctly
- [ ] Images with transparency display properly
- [ ] High-resolution images scale appropriately

## Interaction States

### Mouse Interactions
- [ ] Hover state shows shadow effect
- [ ] Image scale effect works on hover
- [ ] Cursor changes to pointer
- [ ] Click triggers callback with correct artwork data
- [ ] Visual feedback is smooth and performant

### Keyboard Navigation
- [ ] Card is focusable with tab key
- [ ] Focus indicator is visible and styled correctly
- [ ] Enter key triggers artwork selection
- [ ] Space key triggers artwork selection
- [ ] Focus states match design system

## Conditional Rendering

### Optional Fields
- [ ] Card renders correctly without department
- [ ] Card renders correctly without medium
- [ ] Card renders correctly without creation date
- [ ] Card renders correctly without image
- [ ] Footer only shows when department or medium exist

### Attribution States
- [ ] Shows attribution indicator for uncertain attributions
- [ ] Hides attribution indicator for confident attributions
- [ ] Maintains consistent spacing in both states
- [ ] Attribution indicator has correct color and opacity
- [ ] Text alignment remains consistent with/without indicator

## Responsive Design

### Layout Behavior
- [ ] Card maintains proper width across screen sizes
- [ ] Image aspect ratio remains consistent
- [ ] Text remains readable at all sizes
- [ ] Spacing scales appropriately
- [ ] Touch targets are appropriately sized on mobile

### Grid Integration
- [ ] Card works correctly in different grid layouts
- [ ] Maintains consistent sizing with sibling cards
- [ ] Handles different container widths appropriately
- [ ] Maintains proper gaps between cards
- [ ] Responsive behavior matches design specifications

## Accessibility

### Screen Reader Support
- [ ] Card announces as interactive element
- [ ] Image has appropriate alt text
- [ ] Attribution uncertainty is properly announced
- [ ] Department and medium are properly announced
- [ ] Interactive state is clear to assistive technology

### Keyboard Accessibility
- [ ] Focus order is logical
- [ ] Focus indicator is clearly visible
- [ ] Key interactions work consistently
- [ ] No keyboard traps
- [ ] Focus management follows best practices

## Visual Design

### Typography
- [ ] Font sizes match design system
- [ ] Text colors have proper contrast
- [ ] Line heights are consistent
- [ ] Font weights are correct
- [ ] Text alignment is consistent

### Spacing and Layout
- [ ] Padding is consistent with design system
- [ ] Vertical rhythm is maintained
- [ ] Element spacing is consistent
- [ ] Card dimensions match specifications
- [ ] Content alignment is consistent

### Animation and Transitions
- [ ] Hover shadow transition is smooth
- [ ] Image scale transition is smooth
- [ ] Transitions have appropriate duration
- [ ] Animations respect reduced motion preferences
- [ ] Transitions don't cause layout shifts

## Performance

### Rendering Performance
- [ ] Card renders quickly
- [ ] Hover effects are smooth
- [ ] Image loading doesn't cause layout shifts
- [ ] Transitions perform well on lower-end devices
- [ ] Multiple cards render efficiently in grid

### Memory Management
- [ ] Image memory usage is reasonable
- [ ] No memory leaks on mount/unmount
- [ ] Event listeners are properly cleaned up
- [ ] Handles rapid mount/unmount cycles
- [ ] Performance remains stable over time

## Notes for Testers
- Test with various artwork data combinations
- Verify behavior with missing optional fields
- Test across different viewport sizes
- Check performance with large image assets
- Verify accessibility with screen readers
- Document any visual inconsistencies
- Test with both mouse and keyboard navigation
- Verify reduced motion preference handling

Last Updated: January 11, 2025