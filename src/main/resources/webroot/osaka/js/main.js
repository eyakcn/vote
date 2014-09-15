$(function() {
  var $preview = $('#img-preview');
  $preview.hide();

  $('body').on('click.choice', 'div.choice', function(e) {
    var $choice = $(e.target);
    var $question = $choice.parents('.question:first');
    var $answer = $question.find('.answer:first');

    var choice = $choice.data('key');
    $answer.text(choice);
    $answer.removeClass('empty');
    $answer.data('full', $choice.text());
  });

  $('body').on('input.textarea', 'textarea.answer', function(e) {
    var $textarea = $(e.target);
    var empty = !$.trim($textarea.val()).length;
    $textarea.toggleClass('empty', empty);
  });

  $('body').on('click.img-link', 'div.img-link', function(e) {
    var $link = $(e.target);
    var $preview = $('#img-preview');
    var src = $link.data('src');
    $preview.find('img:first').attr('src', src);
    $preview.show();
    return false;
  });

  $('body').on('click.other', function() {
    var $preview = $('#img-preview');
    $preview.hide();
  });
});