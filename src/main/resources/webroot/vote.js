$(function() {
  var openid = $('#user>input[name="openid"]').val();
  var title = $('#content input[name="title"]').val();
  var minSelection = $('#content input[name="minSelection"]').val();
  var maxSelection = $('#content input[name="maxSelection"]').val();
  var getSelectionCount = function() {
    return $('input[type="checkbox"]:checked').length;
  };

  $('input[type="checkbox"]').click(function(e) {
    var selection = getSelectionCount();
    if (maxSelection > 0 && selection > maxSelection) {
      $(this).prop('checked', false);
      alert('您当前选择已满！');
    }
  });

  $('#submit').click(function(e) {
    var selection = getSelectionCount();
    if (minSelection > 0 && selection < minSelection) {
      alert('您当前选择不足！');
      return;
    }

    var result = {};
    result.openid = openid;
    result.title = title;
    result.time = (new Date()).toISOString();
    result.selections = [];

    $('input[type="checkbox"]:checked').each(function() {
      var caption = $(this).next().val();
      result.selections.push(caption);
    });

    $.ajax({
      url: $(location).attr('pathname'),
      type: "POST",
      data: JSON.stringify(result),
      contentType: "application/json; charset=utf-8",
      dataType: "json",
      success: function() {
        confirm("已提交，谢谢参与！");
      },
      error: function() {
        confirm("提交失败，请重试！");
      }
    });
  });
});