$(function() {
  var $preview = $('#img-preview');
  $preview.hide();

  $('body').on('click.choice', 'div.choice', function(e) {
    if (admin) {
      return;
    }
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
      var answerText = $answer.val().trim();
      var maxLength = $answer.attr('maxlength');
      if (answerText.length > maxLength) {
        answerText.substring(0, maxLength);
      }
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
    $('textarea').prop('readonly', true);
    $('[non-admin]').hide();
    $('.answer').val('');
    $('.answer').trigger('input');
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
    $('.answer').trigger('input');

    $('.question').each(function() {
      var $question = $(this);
      var $answer = $(this).find('.answer:first');
      $answer.hide();

      var questionNumber = $question.data('number');
      var answerMap = statistic[questionNumber - 1];

      if ($question.find('.choice').length > 0) {
        $question.find('.choice').each(function() {
          var $choice = $(this);
          var key = $choice.data('key');
          var count = answerMap[key] || 0;

          var $count = $choice.find('> .count');
          if ($count.length == 0) {
            $count = $('<div class="count"></div>');
            $choice.append($count);
          }
          $count.text(count);
          $count.toggleClass('empty', count == 0);
        });
      } else {
        var newCnt = 0;
        $.each(answerMap, function(choice, count) {
          if (choice == '') {
            return true; // continue;
          }
          newCnt++;
          var $choice = $('<div class="choice col-md-2"></div>');
          $choice.text(newCnt + '. ' + choice);
          $count = $('<div class="count"></div>');
          $choice.append($count);
          $count.text(count);
          $count.toggleClass('empty', count == 0);

          $choice.insertBefore($answer);
        });
      }

      var total = 0;
      $.each(answerMap, function(choice, count){
        total += count;
      });
      var emptyCount = answerMap[''] || 0;
      var $count = $question.find('> .count');
      if ($count.length == 0) {
        $count = $('<div class="count"></div>');
        $question.append($count);
      }
      $count.text(total + ' - ' + emptyCount + ' = ' + (total - emptyCount));
      $count.toggleClass('empty', total == 0);
    });
  });

  $('#answer-switch').change(function() {
    var idx = $(this).val() - 1;
    var answers = answersAry[idx];
    $('.question').each(function() {
      var $question = $(this);
      var $answer = $(this).find('.answer:first');
      $answer.show();

      var questionNumber = $question.data('number');
      var answerText = answers[questionNumber - 1];

      $answer.val(answerText);
      $answer.trigger('input');
    });
  });

  $('body').keydown(function(e) {
    if (!admin) {
      return;
    }
    var $switch = $('#answer-switch')
    var minVal = $switch.attr('min');
    var maxVal = $switch.attr('max');
    var current = $switch.val();
    if (current == '') {
      current = 0;
    }
    switch (e.keyCode) {
    case 37: // left
      if (current > minVal) {
        current--;
        $switch.val(current);
        $switch.trigger('change');
      }
      break;
    case 39: // right
      if (current < maxVal) {
        current++;
        $switch.val(current);
        $switch.trigger('change');
      }
      break;
    }
  });
});