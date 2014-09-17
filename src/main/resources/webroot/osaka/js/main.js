$(function() {
  var $preview = $('#img-preview');
  $preview.hide();

  $('body').on('click.choice', 'div.choice', function(e) {
    var $choice = $(e.target);
    var $question = $choice.parents('.question:first');
    var $answer = $question.find('.answer:first');

    $answer.val($choice.data('key'));
    $answer.trigger('input');
    $answer.data('full', $choice.text());
  });

  $('body').on('input.answer', '.answer', function(e) {
    var $input = $(e.target);
    var empty = $.trim($input.val()).length == 0;
    $input.toggleClass('empty', empty);
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

  $('#submit-btn').click(function() {
    $(this).hide();
    var result = [];
    $('.question').each(function() {
      var $question = $(this);
      var $answer = $(this).find('.answer:first');

      var questionNumber = $question.data('number');
      var answerText = $answer.val();

      result.push(answerText);
    });
    $.ajax({
      url: '/osaka/counting',
      type: "POST",
      data: JSON.stringify(result),
      contentType: "application/json; charset=utf-8",
      dataType: "json",
      success: function() {
        confirm("提出しました！どうもありがとうございます。");
      }
    });
  });

  var admin = false;
  $('#admin').click(function() {
    if (admin) {
      return;
    }
    var code = prompt("", "");
    if (code == 'woaiguangjian') {
      admin = true;
      switchToAdmin();
    }
  });

  function switchToAdmin() {
    $('.choice').removeClass('choice');
    $('textarea').prop('readonly', true);
    $('[non-admin]').hide();
    $('.answer').val('');
    $.get('/osaka/counting', function(data) {
      statistic = data['statistic'];
      answersAry = data['answers'];
      if (answersAry && answersAry.length > 0) {
        $('#answer-switch').attr('max', answersAry.length);
        $('#admin').find('div:first').show();
      }
    });
  }

  $('#statistic-btn').click(function() {
    $('.answer').val('');
    // TODO statistics
  });

  $('#answer-switch').change(function() {
    var idx = $(this).val() - 1;
    var answers = answersAry[idx];
    $('.question').each(function() {
      var $question = $(this);
      var $answer = $(this).find('.answer:first');

      var questionNumber = $question.data('number');
      var answerText = answers[questionNumber - 1];

      $answer.val(answerText);
      $answer.trigger('input');
    });
  });
});