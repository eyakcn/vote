$(function () {
  var openid = getParameterByName('openid');
  var contentid = $('#content>input[name="id"]').val();

  var minSelection = $('#content input[name="minSelection"]').val();
  var maxSelection = $('#content input[name="maxSelection"]').val();

  var getSelectionCount = function () {
    return $('input[type="checkbox"]:checked').length;
  };

  $('input[type="checkbox"]').click(function (e) {
    var selection = getSelectionCount();
    if (maxSelection > 0 && selection > maxSelection) {
      $(this).prop('checked', false);
      alert('您当前选择已满！');
    }
  });

  $('#submit').click(function (e) {
    var selection = getSelectionCount();
    if (minSelection > 0 && selection < minSelection) {
      alert('您当前选择不足！');
      return;
    }
    $('#submit').prop('disabled', true);

    var result = {
      'openid': openid,
      'time': (new Date()).toISOString(),
      'selections': []
    };

    $('input[type="checkbox"]:checked').each(function () {
      var caption = $(this).next().val();
      result['selections'].push(caption);
    });

    $.ajax({
      url: $(location).attr('pathname') + "?content-id=" + contentid,
      type: "POST",
      data: JSON.stringify(result),
      contentType: "application/json; charset=utf-8",
      dataType: "json",
      success: function () {
        $('#submit').prop('disabled', false);
        confirm("已提交，谢谢参与！");
      },
      error: function (e) {
        $('#submit').prop('disabled', false);
        confirm("提交失败，请重试！");
        console.log(e);
      }
    });
  });

  var countingUrl = 'vote?content-id=' + contentid + ((openid && openid.length > 0) ? ('&openid=' + openid) : '');
  var source = new EventSource(countingUrl);
  source.addEventListener('message', function (e) {
    console.log(e.data);
    var counting = JSON.parse(e.data);

    var $title = $('article>details>summary>a');
    var oldTitle = $title.text();
    var newTitle = oldTitle.replace(/\(参与人数：\d+\)/g, '(参与人数：' + counting['total'] + ')');
    $title.text(newTitle);

    $('section>details>summary').each(function () {
      var $caption = $('a', this);
      var oldCaption = $caption.text();
      var pureCaption = oldCaption.replace(/\(票数：\d+\)/g, '');
      var newCaption = oldCaption.replace(/\(票数：\d+\)/g, '(票数：' + counting[pureCaption] + ')');
      $caption.text(newCaption);
    });

  }, false);

  source.addEventListener('open', function (e) {
    console.log("EventSource connection open.")
  }, false);

  source.addEventListener('error', function (e) {
    if (e.readyState == EventSource.CLOSED) {
      console.log("EventSource connection closed.")
    }
  }, false);

  function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
      results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
  }

  $('#expand-btn').click(function () {
    $('details').each(function () {
      $(this).prop('open', true);
    });
  });
  $('#collapse-btn').click(function () {
    $('details').each(function () {
      $(this).prop('open', false);
    });
  });
});