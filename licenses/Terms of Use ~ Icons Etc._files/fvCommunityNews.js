/**
 * @package 	FV Community News
 * @version 	2.0
 * @author 		Frank Verhoeven
 * @copyright 	Coyright (c) 2008, Frank Verhoeven
 */

jQuery(document).ready( function($) {
	$('#fvCommunityNewsForm').submit(function() {
		
		// Disable AJAX for image uploading.
		if ('' != $('#fvCommunityNewsImage').val() && $('#fvCommunityNewsImageCheck').val())
			return true;
		
		$('#fvCommunityNewsLoader').fadeIn('slow');
		
		var url = $('#fvCommunityNews').val() + '?fvCommunityNewsAjaxRequest=true';
		var data = $('#fvCommunityNewsForm').serialize();
		
		$.ajax({
			type: 'POST',
			url: url,
			data: data,
			success: function(response) {
				$('#fvCommunityNewsLoader').hide();
				
				$('fvCommunityNewsAjaxResponse', response).each(function(){
					var message = $('message', this).text();
					
					$('#fvCommunityNewsForm > .error').each(function() {
						$(this).removeClass('error');
					});
					
					
					if ('error' == $('status', this).text()) {
						$('errorfields > field', this).each(function() {
							$('#' + $(this).text()).addClass('error');
						});
						
						$('#fvCommunityNewsErrorResponse').html( message );
					} else {
						$('#fvCommunityNewsErrorResponse').hide();
						
						$('#fvCommunityNewsForm').slideUp('slow');
						$('#fvCommunityNewsAjaxResponse').html('<p>' + message + '</p>');
						$('#fvCommunityNewsAjaxResponse').fadeIn('slow');
					}
					
				});
			},
			error: function() {
				$('#fvCommunityNewsAjaxResponse').html('<p>Unable to add your submission, please try again later.</p>');
				$('#fvCommunityNewsLoader').fadeOut('slow');
			}
		});
		
		return false;
	});
	
	$('#fvCommunityNewsCaptchaReloadLink').click(function() {
		var element = $('#fvCommunityNewsCaptchaImage');
		var newSource = element.attr('src') + '&amp;dummy=true';
		
		$('#fvCommunityNewsCaptchaLoader').show();
		
		element.fadeOut();
		element.attr('src', function() {
			return newSource;
		});
		element.load(function() {
			element.fadeIn('slow');
			$('#fvCommunityNewsCaptchaLoader').hide();
		});
		
		return false;
	});
	
});
